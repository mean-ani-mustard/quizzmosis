package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.QuizStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizStatsDAOIT extends DatabaseTestBase {

    private final QuizStatsDAO stats = new QuizStatsDAO();
    private int alice;
    private int bob;
    private int quiz;

    @BeforeEach
    void createUsersAndQuiz() {
        alice = createUser("alice");
        bob = createUser("bob");
        quiz = createQuiz(alice, "Stats quiz");
        for (int i = 1; i <= 4; i++) {
            addTypedQuestion(quiz, "Question " + i, "answer " + i);
        }
    }

    @Test
    void quizThatNobodyPlayedHasEmptyStatistics() {
        QuizStats result = stats.getStats(quiz);

        assertEquals(4, result.getQuestionCount());
        assertEquals(0, result.getAttemptCount());
        assertEquals(0.0, result.getAveragePercent(), 0.001);
        assertEquals(0, result.getHighestScore());
        assertEquals(0.0, result.getAverageDurationSeconds(), 0.001);
    }

    @Test
    void statisticsSummariseAllAttempts() {
        saveAttempt(alice, quiz, 2, 4, 40);
        saveAttempt(bob, quiz, 4, 4, 20);

        QuizStats result = stats.getStats(quiz);

        assertEquals(2, result.getAttemptCount());
        assertEquals(75.0, result.getAveragePercent(), 0.001);
        assertEquals(30.0, result.getAverageDurationSeconds(), 0.001);
        assertEquals(4, result.getHighestScore());
    }

    @Test
    void ownAttemptsCanBeSortedByDateScoreOrTime() {
        int slowPerfect = saveAttempt(alice, quiz, 4, 4, 90);
        int fastWeak = saveAttempt(alice, quiz, 1, 4, 10);
        int newest = saveAttempt(alice, quiz, 3, 4, 50);
        saveAttempt(bob, quiz, 4, 4, 5);

        assertEquals(List.of(newest, fastWeak, slowPerfect), ids(stats.findUserAttempts(alice, quiz, "date")));
        assertEquals(List.of(slowPerfect, newest, fastWeak), ids(stats.findUserAttempts(alice, quiz, "score")));
        assertEquals(List.of(fastWeak, newest, slowPerfect), ids(stats.findUserAttempts(alice, quiz, "time")));
        assertEquals(ids(stats.findUserAttempts(alice, quiz, "date")),
                ids(stats.findUserAttempts(alice, quiz, "score; DROP TABLE users")), "unknown sort -> by date");
    }

    @Test
    void topPlayersOfTheLastDayIgnoreOlderResults() {
        int old = saveAttempt(alice, quiz, 4, 4, 30);
        backdateAttempt(old, 3);
        saveAttempt(alice, quiz, 2, 4, 30);
        saveAttempt(bob, quiz, 3, 4, 30);

        assertEquals(List.of("alice", "bob"), usernames(stats.findTopPerformers(quiz, false, 10)));
        assertEquals(List.of("bob", "alice"), usernames(stats.findTopPerformers(quiz, true, 10)));
    }

    private static List<Integer> ids(List<AttemptSummary> rows) {
        return rows.stream().map(AttemptSummary::getAttemptId).toList();
    }

    private static List<String> usernames(List<AttemptSummary> rows) {
        return rows.stream().map(AttemptSummary::getUsername).toList();
    }
}
