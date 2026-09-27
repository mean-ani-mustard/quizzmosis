package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptAnswer;
import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.QuizAttempt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Reads and writes finished quiz attempts ({@code quiz_attempts} and {@code attempt_answers}). */
public class QuizAttemptDAO {

    /** Columns needed by {@link #mapSummary}. Also used by QuizStatsDAO and LeaderboardDAO. */
    static final String SUMMARY_COLUMNS =
            "a.attempt_id, a.user_id, u.username, u.display_name, a.quiz_id, q.title AS quiz_title, "
                    + "a.score, a.total_questions, a.duration_seconds, a.attempt_date";

    static final String SUMMARY_FROM =
            " FROM quiz_attempts a "
                    + "JOIN users u ON u.user_id = a.user_id "
                    + "JOIN quizzes q ON q.quiz_id = a.quiz_id ";

    private static final String SELECT_SUMMARY = "SELECT " + SUMMARY_COLUMNS + SUMMARY_FROM;

    private static final String NEWEST_FIRST = " ORDER BY a.attempt_date DESC, a.attempt_id DESC";

    /**
     * Saves a finished attempt and all of its answers in one transaction.
     *
     * @return the new attempt id (also set on {@code attempt})
     */
    public int saveFinishedAttempt(QuizAttempt attempt, List<AttemptAnswer> answers) {
        return Jdbc.inTransaction(connection -> {
            int attemptId = Jdbc.insert(connection,
                    "INSERT INTO quiz_attempts (user_id, quiz_id, score, total_questions, duration_seconds) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    attempt.getUserId(), attempt.getQuizId(), attempt.getScore(),
                    attempt.getTotalQuestions(), attempt.getDurationSeconds());
            for (AttemptAnswer answer : answers) {
                Jdbc.update(connection,
                        "INSERT INTO attempt_answers "
                                + "(attempt_id, question_id, user_answer_text, correct_answer, is_correct) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        attemptId, answer.getQuestionId(), answer.getUserAnswer(),
                        answer.getCorrectAnswer(), answer.isCorrect());
            }
            attempt.setAttemptId(attemptId);
            return attemptId;
        });
    }

    public QuizAttempt findById(int attemptId) {
        return Jdbc.queryOne("SELECT attempt_id, user_id, quiz_id, score, total_questions, duration_seconds, "
                + "attempt_date FROM quiz_attempts WHERE attempt_id = ?", QuizAttemptDAO::map, attemptId);
    }

    /** The graded answers of an attempt, in the order the questions were shown. */
    public List<AttemptAnswer> findAnswers(int attemptId) {
        return Jdbc.query("SELECT aa.attempt_id, aa.question_id, q.question_text, "
                        + "aa.user_answer_text, aa.correct_answer, aa.is_correct "
                        + "FROM attempt_answers aa JOIN questions q ON q.question_id = aa.question_id "
                        + "WHERE aa.attempt_id = ? ORDER BY aa.attempt_answer_id",
                QuizAttemptDAO::mapAnswer, attemptId);
    }

    /**
     * One page of a user's quiz history, newest first. Like every list, it leaves out quizzes the
     * user can't see (any more), e.g. because the author made them private.
     */
    public List<AttemptSummary> findHistory(int userId, int limit, int offset) {
        return Jdbc.query(SELECT_SUMMARY + "WHERE a.user_id = ? AND " + QuizDAO.VISIBLE_TO + NEWEST_FIRST
                        + " LIMIT ? OFFSET ?",
                QuizAttemptDAO::mapSummary, userId, userId, userId, limit, offset);
    }

    /** How many attempts {@link #findHistory} can show in total. */
    public int countHistory(int userId) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quiz_attempts a JOIN quizzes q ON q.quiz_id = a.quiz_id "
                + "WHERE a.user_id = ? AND " + QuizDAO.VISIBLE_TO, userId, userId, userId);
    }

    /** Whether the user has finished this quiz at least once (only then may they rate it). */
    public boolean hasFinished(int userId, int quizId) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ? AND quiz_id = ?", userId, quizId) > 0;
    }

    /** The latest attempts of {@code userId}, at quizzes that {@code viewerId} may see. */
    public List<AttemptSummary> findRecentByUser(int userId, int viewerId, int limit) {
        return Jdbc.query(SELECT_SUMMARY + "WHERE a.user_id = ? AND " + QuizDAO.VISIBLE_TO + NEWEST_FIRST + " LIMIT ?",
                QuizAttemptDAO::mapSummary, userId, viewerId, viewerId, limit);
    }

    /** The latest attempts made by the friends of {@code userId}, at quizzes {@code userId} may see. */
    public List<AttemptSummary> findRecentByFriendsOf(int userId, int limit) {
        return Jdbc.query(SELECT_SUMMARY
                        + "JOIN friends f ON f.friend_id = a.user_id AND f.user_id = ? AND f.status = 'accepted' "
                        + "WHERE " + QuizDAO.VISIBLE_TO + NEWEST_FIRST + " LIMIT ?",
                QuizAttemptDAO::mapSummary, userId, userId, userId, limit);
    }

    public int countByUser(int userId) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ?", userId);
    }

    public int count() {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quiz_attempts");
    }

    /** The best score anyone ever got on a quiz (0 if nobody took it). */
    public int highestScore(int quizId) {
        return Jdbc.queryInt("SELECT COALESCE(MAX(score), 0) FROM quiz_attempts WHERE quiz_id = ?", quizId);
    }

    private static QuizAttempt map(ResultSet rs) throws SQLException {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setAttemptId(rs.getInt("attempt_id"));
        attempt.setUserId(rs.getInt("user_id"));
        attempt.setQuizId(rs.getInt("quiz_id"));
        attempt.setScore(rs.getInt("score"));
        attempt.setTotalQuestions(rs.getInt("total_questions"));
        attempt.setDurationSeconds(rs.getInt("duration_seconds"));
        attempt.setAttemptDate(rs.getTimestamp("attempt_date"));
        return attempt;
    }

    private static AttemptAnswer mapAnswer(ResultSet rs) throws SQLException {
        AttemptAnswer answer = new AttemptAnswer();
        answer.setAttemptId(rs.getInt("attempt_id"));
        answer.setQuestionId(rs.getInt("question_id"));
        answer.setQuestionText(rs.getString("question_text"));
        answer.setUserAnswer(rs.getString("user_answer_text"));
        answer.setCorrectAnswer(rs.getString("correct_answer"));
        answer.setCorrect(rs.getBoolean("is_correct"));
        return answer;
    }

    /** Maps a row selected with {@link #SUMMARY_COLUMNS}. */
    static AttemptSummary mapSummary(ResultSet rs) throws SQLException {
        AttemptSummary summary = new AttemptSummary();
        summary.setAttemptId(rs.getInt("attempt_id"));
        summary.setUserId(rs.getInt("user_id"));
        summary.setUsername(rs.getString("username"));
        summary.setDisplayName(rs.getString("display_name"));
        summary.setQuizId(rs.getInt("quiz_id"));
        summary.setQuizTitle(rs.getString("quiz_title"));
        summary.setScore(rs.getInt("score"));
        summary.setTotalQuestions(rs.getInt("total_questions"));
        summary.setDurationSeconds(rs.getInt("duration_seconds"));
        summary.setAttemptDate(rs.getTimestamp("attempt_date"));
        return summary;
    }
}
