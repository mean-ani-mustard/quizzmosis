package com.quizzmosis.dao;

/**
 * Star ratings of quizzes (the {@code quiz_ratings} table): one rating from 1 to 5 per player
 * and quiz. The averages are part of every quiz query in {@link QuizDAO}.
 */
public class QuizRatingDAO {

    /** Saves the user's rating of a quiz, replacing the one they gave before. */
    public void rate(int quizId, int userId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("A rating goes from 1 to 5 stars, not " + rating);
        }
        Jdbc.update("INSERT INTO quiz_ratings (quiz_id, user_id, rating) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE rating = ?", quizId, userId, rating, rating);
    }

    /** The user's rating of a quiz, or {@code null} if they haven't rated it. */
    public Integer findRating(int quizId, int userId) {
        return Jdbc.queryOne("SELECT rating FROM quiz_ratings WHERE quiz_id = ? AND user_id = ?",
                rs -> rs.getInt("rating"), quizId, userId);
    }
}
