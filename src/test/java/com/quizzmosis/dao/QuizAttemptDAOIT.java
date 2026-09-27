package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizAttemptDAOIT extends DatabaseTestBase {

    private int alice;
    private int quiz;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        quiz = createQuiz(alice, "Capitals");
    }

    @Test
    void historyShowsTheNewestAttemptFirst() {
        int yesterday = saveAttempt(alice, quiz, 1, 2, 30);
        backdateAttempt(yesterday, 1);
        int today = saveAttempt(alice, quiz, 2, 2, 25);

        List<AttemptSummary> history = attemptDAO.findHistory(alice, 10, 0);

        assertEquals(List.of(today, yesterday), history.stream().map(AttemptSummary::getAttemptId).toList());
        assertEquals("Capitals", history.get(0).getQuizTitle());
        assertEquals(100, history.get(0).getPercentage());
        assertEquals(List.of(today), attemptDAO.findRecentByUser(alice, alice, 1).stream()
                .map(AttemptSummary::getAttemptId).toList());
        assertEquals(2, attemptDAO.countByUser(alice));
        assertEquals(2, attemptDAO.countHistory(alice));
        assertEquals(List.of(yesterday), attemptDAO.findHistory(alice, 10, 1).stream()
                .map(AttemptSummary::getAttemptId).toList(), "the second page starts after the offset");
        assertTrue(attemptDAO.hasFinished(alice, quiz));
        assertFalse(attemptDAO.hasFinished(alice, createQuiz(alice, "Not played")));
    }

    @Test
    void friendsActivityOnlyShowsAcceptedFriends() {
        FriendsDAO friends = new FriendsDAO();
        int bob = createUser("bob");
        int carol = createUser("carol");
        int dave = createUser("dave");
        friends.sendRequest(alice, bob);
        friends.acceptRequest(bob, alice);
        friends.sendRequest(alice, carol);
        saveAttempt(bob, quiz, 1, 1, 10);
        saveAttempt(carol, quiz, 1, 1, 10);
        saveAttempt(dave, quiz, 1, 1, 10);

        List<AttemptSummary> activity = attemptDAO.findRecentByFriendsOf(alice, 10);

        assertEquals(List.of("bob"), activity.stream().map(AttemptSummary::getUsername).toList());
    }

    @Test
    void highestScoreIsZeroUntilSomebodyPlays() {
        assertEquals(0, attemptDAO.highestScore(quiz));

        saveAttempt(alice, quiz, 3, 5, 10);
        saveAttempt(alice, quiz, 4, 5, 10);

        assertEquals(4, attemptDAO.highestScore(quiz));
    }
}
