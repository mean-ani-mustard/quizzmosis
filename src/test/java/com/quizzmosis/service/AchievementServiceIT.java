package com.quizzmosis.service;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.dao.DatabaseTestBase;
import com.quizzmosis.model.Achievement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementServiceIT extends DatabaseTestBase {

    private final AchievementService achievements = new AchievementService();
    private final AchievementDAO achievementDAO = new AchievementDAO();
    private int alice;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
    }

    @Test
    void authorAchievementsAreEarnedAtOneFiveAndTenQuizzes() {
        List<String> earnedAt = new ArrayList<>();
        for (int count = 1; count <= 10; count++) {
            createQuiz(alice, "Quiz " + count);
            for (Achievement achievement : achievements.onQuizCreated(alice)) {
                earnedAt.add(count + ": " + achievement.getName());
            }
        }

        assertEquals(List.of("1: Amateur Author", "5: Prolific Author", "10: Prodigious Author"), earnedAt);
    }

    @Test
    void finishingTenQuizzesEarnsQuizMachine() {
        int quiz = createQuiz(alice, "Practice");

        for (int i = 1; i <= 9; i++) {
            saveAttempt(alice, quiz, 0, 1, 10);
            assertTrue(achievements.onQuizFinished(alice, quiz, 0).isEmpty(), "attempt " + i);
        }
        saveAttempt(alice, quiz, 0, 1, 10);

        assertEquals(List.of("Quiz Machine"), names(achievements.onQuizFinished(alice, quiz, 0)));
    }

    @Test
    void theGreatestNeedsTheHighestScoreOfAQuiz() {
        int bob = createUser("bob");
        int quiz = createQuiz(alice, "Hard quiz");

        saveAttempt(alice, quiz, 3, 5, 60);
        assertEquals(List.of("I am the Greatest"), names(achievements.onQuizFinished(alice, quiz, 3)));

        saveAttempt(bob, quiz, 2, 5, 60);
        assertTrue(achievements.onQuizFinished(bob, quiz, 2).isEmpty(), "worse than alice");

        saveAttempt(bob, quiz, 3, 5, 50);
        assertEquals(List.of("I am the Greatest"), names(achievements.onQuizFinished(bob, quiz, 3)), "a tie counts");
    }

    @Test
    void aScoreOfZeroIsNeverTheGreatest() {
        int quiz = createQuiz(alice, "Nobody knows");
        saveAttempt(alice, quiz, 0, 3, 5);

        assertTrue(achievements.onQuizFinished(alice, quiz, 0).isEmpty());
    }

    @Test
    void anAchievementIsOnlyAwardedOnce() {
        assertTrue(achievementDAO.award(alice, AchievementService.AMATEUR_AUTHOR));
        assertFalse(achievementDAO.award(alice, AchievementService.AMATEUR_AUTHOR));

        List<Achievement> all = achievementDAO.findAllForUser(alice);
        assertEquals(5, all.size(), "the page lists every achievement");
        assertEquals(1, all.stream().filter(Achievement::isEarned).count());
        assertEquals(List.of("Amateur Author"), names(achievementDAO.findEarnedBy(alice)));
    }

    private static List<String> names(List<Achievement> list) {
        return list.stream().map(Achievement::getName).toList();
    }
}
