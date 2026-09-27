package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.QuizStats;

import java.util.List;

/** Statistics shown on a quiz's summary page. */
public class QuizStatsDAO {

    public QuizStats getStats(int quizId) {
        // An aggregate query without GROUP BY always returns exactly one row
        // (COUNT = 0 and the other values NULL when nobody took the quiz yet).
        QuizStats stats = Jdbc.queryOne(
                "SELECT COUNT(*) AS attempt_count, "
                        + "AVG(score * 100.0 / NULLIF(total_questions, 0)) AS avg_percent, "
                        + "AVG(duration_seconds) AS avg_duration, "
                        + "MAX(score) AS highest_score "
                        + "FROM quiz_attempts WHERE quiz_id = ?",
                rs -> {
                    QuizStats s = new QuizStats();
                    s.setAttemptCount(rs.getInt("attempt_count"));
                    s.setAveragePercent(rs.getDouble("avg_percent"));
                    s.setAverageDurationSeconds(rs.getDouble("avg_duration"));
                    s.setHighestScore(rs.getInt("highest_score"));
                    return s;
                },
                quizId);
        stats.setQuestionCount(Jdbc.queryInt("SELECT COUNT(*) FROM questions WHERE quiz_id = ?", quizId));
        return stats;
    }

    /**
     * One user's attempts at a quiz.
     *
     * @param sort "date" (newest first, the default), "score" (best first) or "time" (fastest first).
     *             The ORDER BY clause is picked from this fixed list, never built from user input.
     */
    public List<AttemptSummary> findUserAttempts(int userId, int quizId, String sort) {
        String orderBy = switch (sort == null ? "" : sort) {
            case "score" -> "a.score DESC, a.duration_seconds ASC";
            case "time" -> "a.duration_seconds ASC";
            default -> "a.attempt_date DESC";
        };
        return Jdbc.query("SELECT " + QuizAttemptDAO.SUMMARY_COLUMNS + QuizAttemptDAO.SUMMARY_FROM
                        + "WHERE a.user_id = ? AND a.quiz_id = ? ORDER BY " + orderBy + ", a.attempt_id DESC",
                QuizAttemptDAO::mapSummary, userId, quizId);
    }

    /**
     * The best attempt of each user on a quiz, best users first.
     *
     * @param lastDayOnly only count attempts from the last 24 hours
     */
    public List<AttemptSummary> findTopPerformers(int quizId, boolean lastDayOnly, int limit) {
        String since = lastDayOnly ? " AND a.attempt_date >= NOW() - INTERVAL 1 DAY" : "";
        // ROW_NUMBER() numbers each user's attempts from best to worst; row 1 is their best one.
        return Jdbc.query("SELECT * FROM ("
                        + "SELECT " + QuizAttemptDAO.SUMMARY_COLUMNS + ", "
                        + "ROW_NUMBER() OVER (PARTITION BY a.user_id "
                        + "ORDER BY a.score DESC, a.duration_seconds ASC, a.attempt_id ASC) AS row_num"
                        + QuizAttemptDAO.SUMMARY_FROM
                        + "WHERE a.quiz_id = ?" + since
                        + ") best WHERE best.row_num = 1 "
                        + "ORDER BY best.score DESC, best.duration_seconds ASC, best.attempt_id ASC LIMIT ?",
                QuizAttemptDAO::mapSummary, quizId, limit);
    }

    /** The most recent attempts at a quiz by anyone. */
    public List<AttemptSummary> findRecentAttempts(int quizId, int limit) {
        return Jdbc.query("SELECT " + QuizAttemptDAO.SUMMARY_COLUMNS + QuizAttemptDAO.SUMMARY_FROM
                        + "WHERE a.quiz_id = ? ORDER BY a.attempt_date DESC, a.attempt_id DESC LIMIT ?",
                QuizAttemptDAO::mapSummary, quizId, limit);
    }
}
