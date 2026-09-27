package com.quizzmosis.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizRatingDAOIT extends DatabaseTestBase {

    private final QuizRatingDAO ratings = new QuizRatingDAO();
    private int bob;
    private int quiz;

    @BeforeEach
    void setUp() {
        quiz = createQuiz(createUser("alice"), "World Capitals");
        bob = createUser("bob");
    }

    @Test
    void ratingAgainReplacesTheOldRating() {
        assertNull(ratings.findRating(quiz, bob));

        ratings.rate(quiz, bob, 2);
        ratings.rate(quiz, bob, 4);

        assertEquals(4, ratings.findRating(quiz, bob));
        assertEquals(1, quizDAO.findById(quiz).getRatingCount(), "still one rating per player");
        assertEquals(4.0, quizDAO.findById(quiz).getAverageRating(), 0.001);
    }

    @Test
    void onlyOneToFiveStars() {
        assertThrows(IllegalArgumentException.class, () -> ratings.rate(quiz, bob, 0));
        assertThrows(IllegalArgumentException.class, () -> ratings.rate(quiz, bob, 6));
    }

    @Test
    void ratingsGoWithTheirQuizOrPlayer() {
        ratings.rate(quiz, bob, 5);

        userDAO.delete(bob);

        assertEquals(0, quizDAO.findById(quiz).getRatingCount());
        assertEquals(0, queryInt("SELECT COUNT(*) FROM quiz_ratings"));
    }
}
