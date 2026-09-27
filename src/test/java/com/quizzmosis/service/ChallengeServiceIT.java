package com.quizzmosis.service;

import com.quizzmosis.dao.ChallengeDAO;
import com.quizzmosis.dao.DatabaseTestBase;
import com.quizzmosis.dao.MessageDAO;
import com.quizzmosis.model.Challenge;
import com.quizzmosis.model.ChallengeStatus;
import com.quizzmosis.model.Message;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSession;
import com.quizzmosis.model.QuizVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The life of a challenge: alice and dave challenge their friend bob to carol's quiz, bob accepts
 * or declines, and finishing the quiz sends the challengers his score.
 */
class ChallengeServiceIT extends DatabaseTestBase {

    private final ChallengeService service = new ChallengeService();
    private final ChallengeDAO challenges = new ChallengeDAO();
    private final MessageDAO messages = new MessageDAO();

    private int alice;
    private int bob;
    private int carol;
    private int dave;
    private Quiz quiz;
    private Question question;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        carol = createUser("carol");
        dave = createUser("dave");
        makeFriends(alice, bob);
        makeFriends(dave, bob);
        int quizId = createQuiz(carol, "World Capitals");
        question = addTypedQuestion(quizId, "Capital of Japan?", "Tokyo");
        quiz = quizDAO.findById(quizId);
    }

    @Test
    void aChallengeWaitsForAnAnswer() throws ValidationException {
        int id = service.send(alice, bob, quiz, "  Beat me!  ");

        Challenge challenge = challenges.findById(id);
        assertEquals(ChallengeStatus.PENDING, challenge.getStatus());
        assertEquals("Beat me!", challenge.getMessage());
        assertEquals("alice", challenge.getChallengerName());
        assertEquals("bob", challenge.getChallengedName());
        assertEquals("World Capitals", challenge.getQuizTitle());
        assertEquals(List.of(id), ids(challenges.findPendingFor(bob)));
        assertTrue(challenges.findPendingFor(alice).isEmpty());
    }

    @Test
    void anEmptyMessageGetsADefaultText() throws ValidationException {
        int id = service.send(alice, bob, quiz, " ");

        assertEquals("Can you beat my score?", challenges.findById(id).getMessage());
    }

    @Test
    void onlyFriendsWhoCanSeeTheQuizCanBeChallenged() throws ValidationException {
        assertThrows(ValidationException.class, () -> service.send(alice, carol, quiz, "Hi"), "not friends");

        int diary = createQuiz(alice, "Diary");
        setVisibility(diary, QuizVisibility.PRIVATE);
        Quiz privateQuiz = quizDAO.findById(diary);
        assertThrows(ValidationException.class, () -> service.send(alice, bob, privateQuiz, "Hi"), "bob can't see it");

        service.send(alice, bob, quiz, "Hi");
        assertThrows(ValidationException.class, () -> service.send(alice, bob, quiz, "Again"),
                "the same challenge is still waiting for an answer");
        assertEquals(1, challenges.findPendingFor(bob).size());
    }

    @Test
    void acceptingTellsTheChallengerAndLeavesTheWaitingList() throws ValidationException {
        int id = service.send(alice, bob, quiz, "Go!");

        Challenge accepted = service.accept(id, bob, "  Bring it on  ");

        assertEquals(quiz.getQuizId(), accepted.getQuizId());
        assertTrue(challenges.findPendingFor(bob).isEmpty(), "an answered challenge isn't waiting any more");
        Challenge stored = challenges.findById(id);
        assertEquals(ChallengeStatus.ACCEPTED, stored.getStatus());
        assertEquals("Bring it on", stored.getResponseComment());

        List<Message> updates = messages.findChallengeUpdates(alice);
        assertEquals(1, updates.size());
        assertEquals(MessageType.CHALLENGE_ACCEPTED, updates.get(0).getType());
        assertEquals("Bring it on", updates.get(0).getContent());
        assertEquals("bob", updates.get(0).getSenderDisplayName());

        assertThrows(ValidationException.class, () -> service.accept(id, bob, null), "already answered");
        assertThrows(ValidationException.class, () -> service.decline(id, bob, null), "already answered");
    }

    @Test
    void decliningTellsTheChallengerAndKeepsTheChallenge() throws ValidationException {
        int id = service.send(alice, bob, quiz, "Go!");

        service.decline(id, bob, "");

        Challenge stored = challenges.findById(id);
        assertEquals(ChallengeStatus.DECLINED, stored.getStatus());
        assertNull(stored.getResponseComment(), "no comment was written");
        Message update = messages.findChallengeUpdates(alice).get(0);
        assertEquals(MessageType.CHALLENGE_DECLINED, update.getType());
        assertNull(update.getContent());
        assertEquals(List.of(id), ids(challenges.findHistory(alice, alice, 10)), "declined challenges stay in the history");
        assertEquals(List.of(id), ids(challenges.findHistory(bob, bob, 10)));
    }

    @Test
    void onlyTheChallengedFriendCanAnswer() throws ValidationException {
        int id = service.send(alice, bob, quiz, "Go!");

        assertThrows(ValidationException.class, () -> service.accept(id, alice, null));
        assertThrows(ValidationException.class, () -> service.decline(id, carol, null));
        assertThrows(ValidationException.class, () -> service.accept(123_456, bob, null));
        assertEquals(ChallengeStatus.PENDING, challenges.findById(id).getStatus());
    }

    @Test
    void aChallengeToAQuizThatBecameHiddenCantBeAccepted() throws ValidationException {
        int alicesQuiz = createQuiz(alice, "Alice's quiz");
        int id = service.send(alice, bob, quizDAO.findById(alicesQuiz), "Go!");
        setVisibility(alicesQuiz, QuizVisibility.PRIVATE);

        assertThrows(ValidationException.class, () -> service.accept(id, bob, null));
        assertEquals(ChallengeStatus.PENDING, challenges.findById(id).getStatus());
    }

    @Test
    void finishingTheQuizSendsTheScoreToEveryChallengerWhoWasAccepted() throws ValidationException {
        int fromAlice = service.send(alice, bob, quiz, "Go!");
        int fromDave = service.send(dave, bob, quiz, "Me too!");
        service.accept(fromAlice, bob, null);
        service.accept(fromDave, bob, null);
        int otherQuiz = createQuiz(carol, "Another quiz");
        int notAccepted = service.send(alice, bob, quizDAO.findById(otherQuiz), "Later");

        QuizService quizService = new QuizService();
        QuizSession session = quizService.start(quiz, 0);
        session.recordAnswer(QuizGrader.grade(question, "tokyo"));
        QuizService.FinishedQuiz finished = quizService.finish(bob, session, 5_000);

        assertEquals(List.of("alice", "dave"), finished.challengersNotified().stream().sorted().toList());
        for (int id : new int[] {fromAlice, fromDave}) {
            Challenge completed = challenges.findById(id);
            assertEquals(ChallengeStatus.COMPLETED, completed.getStatus());
            assertEquals(finished.attemptId(), completed.getAttemptId());
            assertEquals(100, completed.getPercentage());
        }
        assertEquals(ChallengeStatus.PENDING, challenges.findById(notAccepted).getStatus());

        Message result = messages.findChallengeUpdates(dave).get(0);
        assertEquals(MessageType.CHALLENGE_FINISHED, result.getType());
        assertEquals(1, result.getScore());

        messages.markAllRead(alice);
        assertEquals(List.of("alice", "dave"), service.onResultCommented(finished.attemptId()).stream().sorted().toList());
        assertEquals(1, messages.countInboxBadge(alice), "the result message is new again, now with the comment");
    }

    @Test
    void playingWithoutAcceptingCompletesNothing() throws ValidationException {
        int id = service.send(alice, bob, quiz, "Go!");
        int attempt = saveAttempt(bob, quiz.getQuizId(), 1, 1, 10);

        assertTrue(service.onQuizFinished(bob, quiz.getQuizId(), attempt).isEmpty());
        assertEquals(ChallengeStatus.PENDING, challenges.findById(id).getStatus());
        assertTrue(service.onResultCommented(attempt).isEmpty());
    }

    @Test
    void historyOnlyShowsChallengesAtQuizzesTheViewerMaySee() throws ValidationException {
        int friendsQuiz = createQuiz(alice, "Friends only");
        setVisibility(friendsQuiz, QuizVisibility.FRIENDS);
        int hidden = service.send(alice, bob, quizDAO.findById(friendsQuiz), "Just for friends");
        int open = service.send(alice, bob, quiz, "Everyone can see this quiz");

        assertEquals(List.of(open, hidden), ids(challenges.findHistory(bob, bob, 10)), "newest first");
        assertEquals(List.of(open, hidden), ids(challenges.findHistory(bob, alice, 10)));
        assertEquals(List.of(open), ids(challenges.findHistory(bob, carol, 10)),
                "carol isn't alice's friend, so the challenge at alice's friends-only quiz stays hidden");
        assertEquals(List.of(open), ids(challenges.findHistory(bob, bob, 1)), "limit");
    }

    private static List<Integer> ids(List<Challenge> list) {
        return list.stream().map(Challenge::getChallengeId).toList();
    }
}
