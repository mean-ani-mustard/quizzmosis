package com.quizzmosis.dao;

import com.quizzmosis.model.Challenge;
import com.quizzmosis.model.ChallengeStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The {@code challenges} table. Users never delete challenges: an answered challenge simply
 * changes its status and becomes part of the challenge history.
 */
public class ChallengeDAO {

    /** A challenge with both names, the quiz title, the result (if any) and the challenger's best score. */
    private static final String SELECT_CHALLENGE =
            "SELECT c.challenge_id, c.challenger_id, cu.display_name AS challenger_name, c.challenged_id, "
                    + "du.display_name AS challenged_name, c.quiz_id, q.title AS quiz_title, c.message, c.status, "
                    + "c.response_comment, c.attempt_id, a.score, a.total_questions, "
                    + "c.created_at, c.responded_at, c.completed_at, "
                    + "(SELECT MAX(ROUND(100 * b.score / NULLIF(b.total_questions, 0))) FROM quiz_attempts b "
                    + " WHERE b.user_id = c.challenger_id AND b.quiz_id = c.quiz_id) AS challenger_best_percent "
                    + "FROM challenges c "
                    + "JOIN users cu ON cu.user_id = c.challenger_id "
                    + "JOIN users du ON du.user_id = c.challenged_id "
                    + "JOIN quizzes q ON q.quiz_id = c.quiz_id "
                    + "LEFT JOIN quiz_attempts a ON a.attempt_id = c.attempt_id ";

    private static final String NEWEST_FIRST = " ORDER BY c.created_at DESC, c.challenge_id DESC";

    public int create(int challengerId, int challengedId, int quizId, String message) {
        return Jdbc.insert("INSERT INTO challenges (challenger_id, challenged_id, quiz_id, message) VALUES (?, ?, ?, ?)",
                challengerId, challengedId, quizId, message);
    }

    /** Whether the same challenge is already waiting for an answer (so it isn't sent twice). */
    public boolean hasPending(int challengerId, int challengedId, int quizId) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM challenges WHERE challenger_id = ? AND challenged_id = ? "
                + "AND quiz_id = ? AND status = 'pending'", challengerId, challengedId, quizId) > 0;
    }

    public Challenge findById(int challengeId) {
        return Jdbc.queryOne(SELECT_CHALLENGE + "WHERE c.challenge_id = ?", ChallengeDAO::map, challengeId);
    }

    /** Challenges waiting for this user's answer, newest first, on quizzes they may still see. */
    public List<Challenge> findPendingFor(int userId) {
        return Jdbc.query(SELECT_CHALLENGE + "WHERE c.challenged_id = ? AND c.status = 'pending' AND "
                        + QuizDAO.VISIBLE_TO + NEWEST_FIRST,
                ChallengeDAO::map, userId, userId, userId);
    }

    /**
     * Accepts or declines a challenge that is still pending.
     *
     * @return {@code false} if it isn't this user's, or was already answered (e.g. a double click)
     */
    public boolean respond(int challengeId, int challengedId, ChallengeStatus answer, String comment) {
        return Jdbc.update("UPDATE challenges SET status = ?, response_comment = ?, responded_at = CURRENT_TIMESTAMP "
                        + "WHERE challenge_id = ? AND challenged_id = ? AND status = 'pending'",
                answer.getDbValue(), comment, challengeId, challengedId) > 0;
    }

    /**
     * Completes every challenge the user accepted for this quiz with the attempt they just
     * finished, and returns those challenges so their challengers can be told.
     */
    public List<Challenge> completeAccepted(int userId, int quizId, int attemptId) {
        Jdbc.update("UPDATE challenges SET status = 'completed', attempt_id = ?, completed_at = CURRENT_TIMESTAMP "
                + "WHERE challenged_id = ? AND quiz_id = ? AND status = 'accepted'", attemptId, userId, quizId);
        return findCompletedBy(attemptId);
    }

    /** The challenges a finished attempt completed (usually none or one). */
    public List<Challenge> findCompletedBy(int attemptId) {
        return Jdbc.query(SELECT_CHALLENGE + "WHERE c.attempt_id = ?" + NEWEST_FIRST, ChallengeDAO::map, attemptId);
    }

    /**
     * A user's challenge history, sent and received, newest first. Only challenges on quizzes
     * the viewer may see are included.
     */
    public List<Challenge> findHistory(int userId, int viewerId, int limit) {
        return Jdbc.query(SELECT_CHALLENGE + "WHERE (c.challenger_id = ? OR c.challenged_id = ?) AND "
                        + QuizDAO.VISIBLE_TO + NEWEST_FIRST + " LIMIT ?",
                ChallengeDAO::map, userId, userId, viewerId, viewerId, limit);
    }

    private static Challenge map(ResultSet rs) throws SQLException {
        Challenge challenge = new Challenge();
        challenge.setChallengeId(rs.getInt("challenge_id"));
        challenge.setChallengerId(rs.getInt("challenger_id"));
        challenge.setChallengerName(rs.getString("challenger_name"));
        challenge.setChallengedId(rs.getInt("challenged_id"));
        challenge.setChallengedName(rs.getString("challenged_name"));
        challenge.setQuizId(rs.getInt("quiz_id"));
        challenge.setQuizTitle(rs.getString("quiz_title"));
        challenge.setMessage(rs.getString("message"));
        challenge.setStatus(ChallengeStatus.fromDbValue(rs.getString("status")));
        challenge.setResponseComment(rs.getString("response_comment"));
        challenge.setAttemptId(Jdbc.nullableInt(rs, "attempt_id"));
        challenge.setScore(Jdbc.nullableInt(rs, "score"));
        challenge.setTotalQuestions(Jdbc.nullableInt(rs, "total_questions"));
        challenge.setChallengerBestPercent(Jdbc.nullableInt(rs, "challenger_best_percent"));
        challenge.setCreatedAt(rs.getTimestamp("created_at"));
        challenge.setRespondedAt(rs.getTimestamp("responded_at"));
        challenge.setCompletedAt(rs.getTimestamp("completed_at"));
        return challenge;
    }
}
