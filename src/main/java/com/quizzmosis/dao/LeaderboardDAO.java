package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.Timeframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Site-wide leaderboard: each user's best attempt per quiz, ranked by percentage so that quizzes
 * of different lengths can be compared fairly.
 */
public class LeaderboardDAO {

    /**
     * @param quizId    only this quiz, or {@code null} for all quizzes
     * @param timeframe only attempts made within this time window
     * @param viewerId  the user looking at the leaderboard: only quizzes they may see count
     */
    public List<AttemptSummary> findTop(Integer quizId, Timeframe timeframe, int viewerId, int limit) {
        List<Object> params = new ArrayList<>();
        String where = filters(quizId, timeframe, viewerId, params);
        params.add(limit);
        return Jdbc.query("SELECT * FROM ("
                        + "SELECT " + QuizAttemptDAO.SUMMARY_COLUMNS + ", "
                        + "ROW_NUMBER() OVER (PARTITION BY a.user_id, a.quiz_id "
                        + "ORDER BY a.score DESC, a.duration_seconds ASC, a.attempt_id ASC) AS row_num"
                        + QuizAttemptDAO.SUMMARY_FROM + where
                        + ") best WHERE best.row_num = 1 "
                        + "ORDER BY best.score / NULLIF(best.total_questions, 0) DESC, best.score DESC, "
                        + "best.duration_seconds ASC, best.attempt_id ASC LIMIT ?",
                QuizAttemptDAO::mapSummary, params.toArray());
    }

    /** The user's single best attempt within the same filters, or {@code null}. */
    public AttemptSummary findPersonalBest(int userId, Integer quizId, Timeframe timeframe) {
        List<Object> params = new ArrayList<>();
        String where = filters(quizId, timeframe, userId, params) + " AND a.user_id = ?";
        params.add(userId);
        return Jdbc.queryOne("SELECT " + QuizAttemptDAO.SUMMARY_COLUMNS + QuizAttemptDAO.SUMMARY_FROM + where
                        + " ORDER BY a.score / NULLIF(a.total_questions, 0) DESC, a.score DESC, "
                        + "a.duration_seconds ASC LIMIT 1",
                QuizAttemptDAO::mapSummary, params.toArray());
    }

    /** Builds the WHERE clause. Only fixed SQL snippets are added; values go into {@code params}. */
    private static String filters(Integer quizId, Timeframe timeframe, int viewerId, List<Object> params) {
        StringBuilder where = new StringBuilder("WHERE a.total_questions > 0 AND " + QuizDAO.VISIBLE_TO);
        params.add(viewerId);
        params.add(viewerId);
        if (quizId != null) {
            where.append(" AND a.quiz_id = ?");
            params.add(quizId);
        }
        switch (timeframe) {
            case TODAY -> where.append(" AND a.attempt_date >= CURDATE()");
            case WEEK -> where.append(" AND a.attempt_date >= NOW() - INTERVAL 7 DAY");
            case MONTH -> where.append(" AND a.attempt_date >= NOW() - INTERVAL 30 DAY");
            case ALL -> { /* no time filter */ }
        }
        return where.toString();
    }
}
