package com.quizzmosis.service;

import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.User;

/**
 * Who may see a quiz (and rate it). The visibility rule is written in SQL in
 * {@code QuizDAO.VISIBLE_TO} for lists; this class answers it for a single quiz, e.g. before
 * showing its page or starting it.
 */
public class QuizAccess {

    private final FriendsDAO friends;
    private final QuizAttemptDAO attempts;

    public QuizAccess() {
        this(new FriendsDAO(), new QuizAttemptDAO());
    }

    public QuizAccess(FriendsDAO friends) {
        this(friends, new QuizAttemptDAO());
    }

    public QuizAccess(FriendsDAO friends, QuizAttemptDAO attempts) {
        this.friends = friends;
        this.attempts = attempts;
    }

    /**
     * Whether the quiz shows up for this user: public quizzes for everyone, "friends only"
     * quizzes for the author's friends, and every quiz for its author.
     */
    public boolean isVisibleTo(Quiz quiz, int userId) {
        if (quiz.getOwnerId() == userId) {
            return true;
        }
        return switch (quiz.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> friends.areFriends(quiz.getOwnerId(), userId);
            case PRIVATE -> false;
        };
    }

    /**
     * Whether the user may open the quiz's page and play it. Admins may open every quiz, so they
     * can check a quiz before deleting it, but hidden quizzes never appear in their lists.
     */
    public boolean canOpen(Quiz quiz, User user) {
        return user.isAdmin() || isVisibleTo(quiz, user.getUserId());
    }

    /** Whether the user may rate the quiz: they finished it, can still see it, and didn't write it. */
    public boolean canRate(Quiz quiz, int userId) {
        return quiz.getOwnerId() != userId && isVisibleTo(quiz, userId)
                && attempts.hasFinished(userId, quiz.getQuizId());
    }
}
