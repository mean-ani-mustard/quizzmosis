package com.quizzmosis.dao;

import com.quizzmosis.model.QuizComment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Comments players leave on a quiz after taking it (the {@code quiz_comments} table). */
public class QuizCommentDAO {

    /** A comment with its author's name and the score of the attempt it was written after. */
    private static final String SELECT_COMMENT =
            "SELECT qc.comment_id, qc.quiz_id, qc.user_id, u.username, u.display_name, qc.attempt_id, "
                    + "a.score, a.total_questions, qc.content, qc.created_at "
                    + "FROM quiz_comments qc "
                    + "JOIN users u ON u.user_id = qc.user_id "
                    + "LEFT JOIN quiz_attempts a ON a.attempt_id = qc.attempt_id ";

    /**
     * Adds a comment written after an attempt.
     *
     * @return the new comment's id, or -1 if that attempt already has a comment
     */
    public int add(int quizId, int userId, int attemptId, String content) {
        try {
            return Jdbc.insert("INSERT INTO quiz_comments (quiz_id, user_id, attempt_id, content) VALUES (?, ?, ?, ?)",
                    quizId, userId, attemptId, content);
        } catch (DataAccessException e) {
            if (e.isConstraintViolation()) {
                return -1;
            }
            throw e;
        }
    }

    /** All comments on a quiz, newest first. */
    public List<QuizComment> findByQuiz(int quizId) {
        return Jdbc.query(SELECT_COMMENT + "WHERE qc.quiz_id = ? ORDER BY qc.created_at DESC, qc.comment_id DESC",
                QuizCommentDAO::map, quizId);
    }

    /** The comment written after this attempt, or {@code null}. */
    public QuizComment findByAttempt(int attemptId) {
        return Jdbc.queryOne(SELECT_COMMENT + "WHERE qc.attempt_id = ?", QuizCommentDAO::map, attemptId);
    }

    public QuizComment findById(int commentId) {
        return Jdbc.queryOne(SELECT_COMMENT + "WHERE qc.comment_id = ?", QuizCommentDAO::map, commentId);
    }

    public boolean delete(int commentId) {
        return Jdbc.update("DELETE FROM quiz_comments WHERE comment_id = ?", commentId) > 0;
    }

    private static QuizComment map(ResultSet rs) throws SQLException {
        QuizComment comment = new QuizComment();
        comment.setCommentId(rs.getInt("comment_id"));
        comment.setQuizId(rs.getInt("quiz_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setUsername(rs.getString("username"));
        comment.setDisplayName(rs.getString("display_name"));
        comment.setAttemptId(Jdbc.nullableInt(rs, "attempt_id"));
        comment.setScore(Jdbc.nullableInt(rs, "score"));
        comment.setTotalQuestions(Jdbc.nullableInt(rs, "total_questions"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(rs.getTimestamp("created_at"));
        return comment;
    }
}
