package com.quizzmosis.service;

import com.quizzmosis.dao.QuestionDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.model.Achievement;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizAttempt;
import com.quizzmosis.model.QuizSession;

import java.util.Collections;
import java.util.List;

/** Starting and finishing quiz attempts. */
public class QuizService {

    /**
     * What finishing a quiz produced: the saved attempt, any achievements earned by it, and the
     * friends who got the result because they had challenged the player to this quiz.
     */
    public record FinishedQuiz(int attemptId, List<Achievement> newAchievements, List<String> challengersNotified) {
    }

    private final QuestionDAO questions;
    private final QuizAttemptDAO attempts;
    private final AchievementService achievements;
    private final ChallengeService challenges;

    public QuizService() {
        this(new QuestionDAO(), new QuizAttemptDAO(), new AchievementService(), new ChallengeService());
    }

    public QuizService(QuestionDAO questions, QuizAttemptDAO attempts, AchievementService achievements,
                       ChallengeService challenges) {
        this.questions = questions;
        this.attempts = attempts;
        this.achievements = achievements;
        this.challenges = challenges;
    }

    /**
     * Loads the questions of a playable quiz and puts them in display order (shuffled once here
     * if the quiz uses random order, so the order stays the same for the whole attempt).
     */
    public QuizSession start(Quiz quiz, long nowMillis) {
        List<Question> list = questions.findByQuiz(quiz.getQuizId());
        if (quiz.isRandomOrder()) {
            Collections.shuffle(list);
        }
        return new QuizSession(quiz, list, nowMillis);
    }

    /**
     * Saves the attempt with all of its answers, checks for new achievements, and completes the
     * challenges the player had accepted for this quiz.
     */
    public FinishedQuiz finish(int userId, QuizSession session, long nowMillis) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(session.getQuizId());
        attempt.setScore(session.getScore());
        attempt.setTotalQuestions(session.getTotalQuestions());
        attempt.setDurationSeconds(session.getElapsedSeconds(nowMillis));

        int attemptId = attempts.saveFinishedAttempt(attempt, session.getAnswersForAllQuestions());
        List<Achievement> earned = achievements.onQuizFinished(userId, session.getQuizId(), attempt.getScore());
        List<String> notified = challenges.onQuizFinished(userId, session.getQuizId(), attemptId);
        return new FinishedQuiz(attemptId, earned, notified);
    }
}
