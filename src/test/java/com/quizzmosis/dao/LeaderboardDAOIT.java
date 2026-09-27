package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeaderboardDAOIT extends DatabaseTestBase {

    private final LeaderboardDAO leaderboard = new LeaderboardDAO();
    private int alice;
    private int bob;
    private int longQuiz;
    private int shortQuiz;

    @BeforeEach
    void createUsersAndQuizzes() {
        alice = createUser("alice");
        bob = createUser("bob");
        longQuiz = createQuiz(alice, "Four questions");
        shortQuiz = createQuiz(alice, "Two questions");
    }

    @Test
    void everyPlayerAppearsOncePerQuizWithTheirBestResult() {
        saveAttempt(alice, longQuiz, 2, 4, 30);
        int best = saveAttempt(alice, longQuiz, 4, 4, 50);
        saveAttempt(alice, longQuiz, 4, 4, 55);
        saveAttempt(bob, longQuiz, 3, 4, 20);

        List<AttemptSummary> top = leaderboard.findTop(longQuiz, Timeframe.ALL, alice, 10);

        assertEquals(List.of("alice", "bob"), usernames(top));
        assertEquals(best, top.get(0).getAttemptId(), "a tie on score goes to the faster attempt");
    }

    @Test
    void rankedByPercentageThenPointsThenTime() {
        int carol = createUser("carol");
        int dave = createUser("dave");
        int erin = createUser("erin");
        saveAttempt(bob, longQuiz, 4, 4, 60);
        saveAttempt(carol, longQuiz, 4, 4, 30);
        saveAttempt(dave, shortQuiz, 2, 2, 10);
        saveAttempt(erin, shortQuiz, 1, 2, 5);

        List<AttemptSummary> top = leaderboard.findTop(null, Timeframe.ALL, alice, 10);

        // 100% with 4 points (carol faster than bob), then 100% with 2 points, then 50%.
        assertEquals(List.of("carol", "bob", "dave", "erin"), usernames(top));
        assertEquals(List.of("carol", "bob"), usernames(leaderboard.findTop(null, Timeframe.ALL, alice, 2)), "limit");
    }

    @Test
    void periodFilterIgnoresOlderResults() {
        int old = saveAttempt(alice, longQuiz, 4, 4, 30);
        backdateAttempt(old, 10);
        saveAttempt(bob, longQuiz, 2, 4, 30);

        assertEquals(List.of("alice", "bob"), usernames(leaderboard.findTop(longQuiz, Timeframe.ALL, alice, 10)));
        assertEquals(List.of("alice", "bob"), usernames(leaderboard.findTop(longQuiz, Timeframe.MONTH, alice, 10)));
        assertEquals(List.of("bob"), usernames(leaderboard.findTop(longQuiz, Timeframe.WEEK, alice, 10)));
        assertEquals(List.of("bob"), usernames(leaderboard.findTop(longQuiz, Timeframe.TODAY, alice, 10)));
    }

    @Test
    void personalBestUsesTheSameFilters() {
        int old = saveAttempt(alice, longQuiz, 4, 4, 30);
        backdateAttempt(old, 10);
        saveAttempt(alice, longQuiz, 1, 4, 30);

        assertEquals(4, leaderboard.findPersonalBest(alice, longQuiz, Timeframe.ALL).getScore());
        assertEquals(1, leaderboard.findPersonalBest(alice, longQuiz, Timeframe.WEEK).getScore());
        assertNull(leaderboard.findPersonalBest(alice, shortQuiz, Timeframe.ALL), "never played it");
    }

    private static List<String> usernames(List<AttemptSummary> rows) {
        return rows.stream().map(AttemptSummary::getUsername).toList();
    }
}
