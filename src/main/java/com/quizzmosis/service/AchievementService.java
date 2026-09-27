package com.quizzmosis.service;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Achievement;

import java.util.ArrayList;
import java.util.List;

/** Decides when users earn achievements. The ids match the rows inserted by db/schema.sql. */
public class AchievementService {

    public static final int AMATEUR_AUTHOR = 1;
    public static final int PROLIFIC_AUTHOR = 2;
    public static final int PRODIGIOUS_AUTHOR = 3;
    public static final int QUIZ_MACHINE = 4;
    public static final int I_AM_THE_GREATEST = 5;

    private final AchievementDAO achievements;
    private final QuizDAO quizzes;
    private final QuizAttemptDAO attempts;

    public AchievementService() {
        this(new AchievementDAO(), new QuizDAO(), new QuizAttemptDAO());
    }

    public AchievementService(AchievementDAO achievements, QuizDAO quizzes, QuizAttemptDAO attempts) {
        this.achievements = achievements;
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    /**
     * Call after a user created a quiz.
     *
     * @return the achievements earned just now (usually none)
     */
    public List<Achievement> onQuizCreated(int userId) {
        int created = quizzes.countByOwner(userId);
        List<Achievement> earned = new ArrayList<>();
        awardIf(created >= 1, userId, AMATEUR_AUTHOR, earned);
        awardIf(created >= 5, userId, PROLIFIC_AUTHOR, earned);
        awardIf(created >= 10, userId, PRODIGIOUS_AUTHOR, earned);
        return earned;
    }

    /**
     * Call after a finished attempt has been saved.
     *
     * @return the achievements earned just now (usually none)
     */
    public List<Achievement> onQuizFinished(int userId, int quizId, int score) {
        List<Achievement> earned = new ArrayList<>();
        awardIf(attempts.countByUser(userId) >= 10, userId, QUIZ_MACHINE, earned);
        // The new attempt is already saved, so "score >= best score" means nobody has done better.
        awardIf(score > 0 && score >= attempts.highestScore(quizId), userId, I_AM_THE_GREATEST, earned);
        return earned;
    }

    private void awardIf(boolean condition, int userId, int achievementId, List<Achievement> earned) {
        if (condition && achievements.award(userId, achievementId)) {
            earned.add(achievements.findById(achievementId));
        }
    }
}
