package com.quizzmosis.dao;

import com.quizzmosis.model.Message;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.NoteThread;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code messages} table: notes between friends (a note plus its replies form a conversation)
 * and the automatic messages that tell a challenger what happened to their challenge.
 */
public class MessageDAO {

    private static final String SELECT_MESSAGE =
            "SELECT m.message_id, m.sender_id, s.display_name AS sender_display_name, "
                    + "m.receiver_id, r.display_name AS receiver_display_name, m.message_type, m.content, "
                    + "m.thread_id, m.challenge_id, m.is_read, m.sent_at "
                    + "FROM messages m "
                    + "JOIN users s ON s.user_id = m.sender_id "
                    + "JOIN users r ON r.user_id = m.receiver_id ";

    /**
     * Unread messages that still count: notes always, challenge messages only while the receiver
     * may see the quiz (it can become private later). Needs the receiver's id three times.
     */
    private static final String UNREAD_FROM =
            "FROM messages m "
                    + "LEFT JOIN challenges c ON c.challenge_id = m.challenge_id "
                    + "LEFT JOIN quizzes q ON q.quiz_id = c.quiz_id "
                    + "WHERE m.receiver_id = ? AND m.is_read = FALSE AND (m.challenge_id IS NULL OR "
                    + QuizDAO.VISIBLE_TO + ")";

    /** Starts a new conversation. */
    public int sendNote(int senderId, int receiverId, String content) {
        return insert(senderId, receiverId, MessageType.NOTE, content, null, null);
    }

    /** Adds a reply to the conversation that {@code threadId} (its first note) started. */
    public int reply(int threadId, int senderId, int receiverId, String content) {
        return insert(senderId, receiverId, MessageType.NOTE, content, threadId, null);
    }

    /** Tells a challenger that their friend accepted, declined or finished the challenge. */
    public int sendChallengeUpdate(MessageType type, int senderId, int receiverId, int challengeId, String comment) {
        return insert(senderId, receiverId, type, comment, null, challengeId);
    }

    /** A single note (for example the first note of a conversation someone replies to), or {@code null}. */
    public Message findNote(int messageId) {
        return Jdbc.queryOne(SELECT_MESSAGE + "WHERE m.message_id = ? AND m.message_type = 'note'",
                MessageDAO::map, messageId);
    }

    /** Every conversation the user takes part in, the most recently active first. */
    public List<NoteThread> findThreads(int userId) {
        List<Message> notes = Jdbc.query(SELECT_MESSAGE
                        + "WHERE m.message_type = 'note' AND (m.sender_id = ? OR m.receiver_id = ?) "
                        + "ORDER BY m.sent_at, m.message_id",
                MessageDAO::map, userId, userId);
        return groupIntoThreads(notes, userId);
    }

    /**
     * Groups notes (oldest first) into conversations: a first note has no thread id, and each
     * reply carries the id of the first note.
     */
    static List<NoteThread> groupIntoThreads(List<Message> notes, int userId) {
        Map<Integer, List<Message>> byThread = new LinkedHashMap<>();
        for (Message note : notes) {
            int threadId = note.getThreadId() != null ? note.getThreadId() : note.getMessageId();
            byThread.computeIfAbsent(threadId, id -> new ArrayList<>()).add(note);
        }
        List<NoteThread> threads = new ArrayList<>();
        for (Map.Entry<Integer, List<Message>> entry : byThread.entrySet()) {
            List<Message> messages = entry.getValue();
            Message first = messages.get(0);
            boolean startedByUser = first.getSenderId() == userId;
            int otherId = startedByUser ? first.getReceiverId() : first.getSenderId();
            String otherName = startedByUser ? first.getReceiverDisplayName() : first.getSenderDisplayName();
            boolean unread = messages.stream().anyMatch(m -> m.getReceiverId() == userId && !m.isRead());
            threads.add(new NoteThread(entry.getKey(), otherId, otherName, messages, unread));
        }
        // The most recently active conversation first; the message id breaks ties within a second.
        threads.sort(Comparator.comparing(NoteThread::getLastActivity)
                .thenComparing(thread -> thread.getLastMessage().getMessageId())
                .reversed());
        return threads;
    }

    /**
     * What happened to the challenges this user sent, newest first: accepted, declined, and
     * finished (with the friend's score and the comment they left on the quiz).
     */
    public List<Message> findChallengeUpdates(int userId) {
        return Jdbc.query("SELECT m.message_id, m.sender_id, s.display_name AS sender_display_name, "
                        + "m.receiver_id, NULL AS receiver_display_name, "
                        + "m.message_type, m.content, m.thread_id, m.challenge_id, m.is_read, m.sent_at, "
                        + "c.quiz_id, q.title AS quiz_title, a.score, a.total_questions, qc.content AS result_comment, "
                        + "(SELECT MAX(ROUND(100 * b.score / NULLIF(b.total_questions, 0))) FROM quiz_attempts b "
                        + " WHERE b.user_id = m.receiver_id AND b.quiz_id = c.quiz_id) AS receiver_best_percent "
                        + "FROM messages m "
                        + "JOIN users s ON s.user_id = m.sender_id "
                        + "JOIN challenges c ON c.challenge_id = m.challenge_id "
                        + "JOIN quizzes q ON q.quiz_id = c.quiz_id "
                        + "LEFT JOIN quiz_attempts a ON a.attempt_id = c.attempt_id "
                        + "LEFT JOIN quiz_comments qc ON qc.attempt_id = c.attempt_id "
                        + "WHERE m.receiver_id = ? AND m.message_type <> 'note' AND " + QuizDAO.VISIBLE_TO
                        + " ORDER BY m.sent_at DESC, m.message_id DESC",
                MessageDAO::mapChallengeUpdate, userId, userId, userId);
    }

    /**
     * The number next to "Messages" in the navigation bar: unread messages plus the friend
     * requests and challenges that are waiting for an answer.
     */
    public int countInboxBadge(int userId) {
        return Jdbc.queryInt("SELECT "
                        + "(SELECT COUNT(*) " + UNREAD_FROM + ") + "
                        + "(SELECT COUNT(*) FROM friends WHERE friend_id = ? AND status = 'pending') + "
                        + "(SELECT COUNT(*) FROM challenges pc JOIN quizzes q ON q.quiz_id = pc.quiz_id "
                        + " WHERE pc.challenged_id = ? AND pc.status = 'pending' AND " + QuizDAO.VISIBLE_TO + ")",
                userId, userId, userId, userId, userId, userId, userId);
    }

    /** Shows the "finished your challenge" messages for this attempt as new again (it got a comment). */
    public void markResultUnread(int attemptId) {
        Jdbc.update("UPDATE messages SET is_read = FALSE WHERE message_type = 'challenge_finished' "
                + "AND challenge_id IN (SELECT challenge_id FROM challenges WHERE attempt_id = ?)", attemptId);
    }

    public void markAllRead(int receiverId) {
        Jdbc.update("UPDATE messages SET is_read = TRUE WHERE receiver_id = ? AND is_read = FALSE", receiverId);
    }

    private int insert(int senderId, int receiverId, MessageType type, String content,
                       Integer threadId, Integer challengeId) {
        return Jdbc.insert("INSERT INTO messages (sender_id, receiver_id, message_type, content, thread_id, challenge_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                senderId, receiverId, type.getDbValue(), content, threadId, challengeId);
    }

    private static Message map(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setMessageId(rs.getInt("message_id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setSenderDisplayName(rs.getString("sender_display_name"));
        message.setReceiverId(rs.getInt("receiver_id"));
        message.setReceiverDisplayName(rs.getString("receiver_display_name"));
        message.setType(MessageType.fromDbValue(rs.getString("message_type")));
        message.setContent(rs.getString("content"));
        message.setThreadId(Jdbc.nullableInt(rs, "thread_id"));
        message.setChallengeId(Jdbc.nullableInt(rs, "challenge_id"));
        message.setRead(rs.getBoolean("is_read"));
        message.setSentAt(rs.getTimestamp("sent_at"));
        return message;
    }

    private static Message mapChallengeUpdate(ResultSet rs) throws SQLException {
        Message message = map(rs);
        message.setQuizId(rs.getInt("quiz_id"));
        message.setQuizTitle(rs.getString("quiz_title"));
        message.setScore(Jdbc.nullableInt(rs, "score"));
        message.setTotalQuestions(Jdbc.nullableInt(rs, "total_questions"));
        message.setResultComment(rs.getString("result_comment"));
        message.setReceiverBestPercent(Jdbc.nullableInt(rs, "receiver_best_percent"));
        return message;
    }
}
