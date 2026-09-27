package com.quizzmosis.dao;

import com.quizzmosis.model.ChallengeStatus;
import com.quizzmosis.model.Message;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.NoteThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDAOIT extends DatabaseTestBase {

    private final MessageDAO messages = new MessageDAO();
    private final ChallengeDAO challenges = new ChallengeDAO();
    private int alice;
    private int bob;
    private int carol;

    @BeforeEach
    void createUsers() {
        alice = createUser("alice");
        bob = createUser("bob");
        carol = createUser("carol");
    }

    @Test
    void repliesAreShownUnderTheNoteThatStartedTheConversation() {
        int first = messages.sendNote(alice, bob, "Hi Bob!");
        int answer = messages.reply(first, bob, alice, "Hi Alice!");
        int question = messages.reply(first, alice, bob, "How are you?");
        int other = messages.sendNote(carol, bob, "Hello from Carol");
        sentMinutesAgo(first, 10);
        sentMinutesAgo(answer, 9);
        sentMinutesAgo(question, 8);
        sentMinutesAgo(other, 20);

        List<NoteThread> threads = messages.findThreads(bob);

        assertEquals(2, threads.size());
        NoteThread withAlice = threads.get(0);
        assertEquals(first, withAlice.getThreadId(), "the most recently active conversation comes first");
        assertEquals(alice, withAlice.getOtherUserId());
        assertEquals("alice", withAlice.getOtherDisplayName());
        assertEquals(List.of("Hi Bob!", "Hi Alice!", "How are you?"), contents(withAlice));
        assertTrue(withAlice.isUnread(), "bob hasn't read alice's notes yet");
        assertEquals("carol", threads.get(1).getOtherDisplayName());

        List<NoteThread> alicesThreads = messages.findThreads(alice);
        assertEquals(1, alicesThreads.size(), "alice only takes part in one conversation");
        assertEquals("bob", alicesThreads.get(0).getOtherDisplayName());
        assertEquals(List.of("Hi Bob!", "Hi Alice!", "How are you?"), contents(alicesThreads.get(0)));
    }

    @Test
    void aReplyMovesItsConversationToTheTop() {
        int older = messages.sendNote(alice, bob, "Old conversation");
        int newer = messages.sendNote(carol, bob, "New conversation");
        sentMinutesAgo(older, 30);
        sentMinutesAgo(newer, 20);
        assertEquals(List.of(newer, older), threadIds(messages.findThreads(bob)));

        messages.reply(older, bob, alice, "Sorry for the late answer");

        assertEquals(List.of(older, newer), threadIds(messages.findThreads(bob)));
    }

    @Test
    void findNoteOnlyFindsNotes() {
        int note = messages.sendNote(alice, bob, "Hi!");
        int quiz = createQuiz(carol, "World Capitals");
        int challenge = challenges.create(alice, bob, quiz, "Beat me");
        int update = messages.sendChallengeUpdate(MessageType.CHALLENGE_DECLINED, bob, alice, challenge, null);

        Message found = messages.findNote(note);
        assertEquals("Hi!", found.getContent());
        assertEquals("alice", found.getSenderDisplayName());
        assertEquals("bob", found.getReceiverDisplayName());
        assertNull(found.getThreadId(), "a first note belongs to no other conversation");
        assertNull(messages.findNote(update));
        assertNull(messages.findNote(123_456));
    }

    @Test
    void challengerHearsHowTheirChallengeWent() {
        int quiz = createQuiz(carol, "World Capitals");
        int challenge = challenges.create(alice, bob, quiz, "Beat me");
        challenges.respond(challenge, bob, ChallengeStatus.ACCEPTED, "On it!");
        messages.sendChallengeUpdate(MessageType.CHALLENGE_ACCEPTED, bob, alice, challenge, "On it!");
        int attempt = saveAttempt(bob, quiz, 3, 4, 30);
        challenges.completeAccepted(bob, quiz, attempt);
        messages.sendChallengeUpdate(MessageType.CHALLENGE_FINISHED, bob, alice, challenge, null);
        new QuizCommentDAO().add(quiz, bob, attempt, "Fun quiz!");
        saveAttempt(alice, quiz, 2, 4, 20);

        List<Message> updates = messages.findChallengeUpdates(alice);

        assertEquals(2, updates.size());
        Message finished = updates.get(0);
        assertEquals(MessageType.CHALLENGE_FINISHED, finished.getType(), "newest first");
        assertEquals("bob", finished.getSenderDisplayName());
        assertEquals("World Capitals", finished.getQuizTitle());
        assertEquals(3, finished.getScore());
        assertEquals(4, finished.getTotalQuestions());
        assertEquals(75, finished.getPercentage());
        assertEquals("Fun quiz!", finished.getResultComment(), "the comment bob left on the quiz");
        assertEquals(50, finished.getReceiverBestPercent(), "alice's own best, to compare");
        assertFalse(finished.isRead());

        Message accepted = updates.get(1);
        assertEquals(MessageType.CHALLENGE_ACCEPTED, accepted.getType());
        assertEquals("On it!", accepted.getContent());

        assertTrue(messages.findChallengeUpdates(bob).isEmpty(), "only the challenger gets these");
        assertTrue(messages.findThreads(alice).isEmpty(), "challenge messages are not notes");
    }

    @Test
    void badgeCountsUnreadMessagesOpenRequestsAndChallengesToAnswer() {
        int first = messages.sendNote(alice, bob, "One");
        messages.reply(first, alice, bob, "Two");
        new FriendsDAO().sendRequest(carol, bob);
        challenges.create(alice, bob, createQuiz(carol, "Quiz"), "Your turn");

        assertEquals(4, messages.countInboxBadge(bob), "two unread notes, a friend request and a challenge");

        messages.markAllRead(bob);

        assertEquals(2, messages.countInboxBadge(bob), "the request and the challenge still wait for an answer");
        assertTrue(messages.findThreads(bob).stream().noneMatch(NoteThread::isUnread));
    }

    @Test
    void aCommentMakesTheResultMessageNewAgain() {
        int quiz = createQuiz(carol, "Quiz");
        int challenge = challenges.create(alice, bob, quiz, "Go!");
        challenges.respond(challenge, bob, ChallengeStatus.ACCEPTED, null);
        int attempt = saveAttempt(bob, quiz, 1, 1, 5);
        challenges.completeAccepted(bob, quiz, attempt);
        messages.sendChallengeUpdate(MessageType.CHALLENGE_FINISHED, bob, alice, challenge, null);
        messages.sendNote(bob, alice, "A note that stays read");
        messages.markAllRead(alice);

        messages.markResultUnread(attempt);

        assertEquals(1, messages.countInboxBadge(alice));
        assertFalse(messages.findChallengeUpdates(alice).get(0).isRead());
    }

    private static void sentMinutesAgo(int messageId, int minutes) {
        execute("UPDATE messages SET sent_at = NOW() - INTERVAL " + minutes + " MINUTE WHERE message_id = ?", messageId);
    }

    private static List<String> contents(NoteThread thread) {
        return thread.getMessages().stream().map(Message::getContent).toList();
    }

    private static List<Integer> threadIds(List<NoteThread> threads) {
        return threads.stream().map(NoteThread::getThreadId).toList();
    }
}
