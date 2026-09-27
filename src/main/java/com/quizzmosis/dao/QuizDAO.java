package com.quizzmosis.dao;

import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSort;
import com.quizzmosis.model.QuizVisibility;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the {@code quizzes} table. */
public class QuizDAO {

    /**
     * Every quiz query selects the owner's name, two counts and the average rating. These are
     * correlated sub-queries rather than JOIN + GROUP BY, so joining questions, attempts and
     * ratings can never multiply each other's rows.
     */
    private static final String SELECT_QUIZ =
            "SELECT q.quiz_id, q.owner_id, u.username AS owner_username, q.title, q.description, "
                    + "q.random_order, q.multiple_pages, q.immediate_correction, q.visibility, "
                    + "q.time_limit_seconds, q.creation_date, "
                    + "(SELECT COUNT(*) FROM questions qs WHERE qs.quiz_id = q.quiz_id) AS question_count, "
                    + "(SELECT COUNT(*) FROM quiz_attempts a WHERE a.quiz_id = q.quiz_id) AS attempt_count, "
                    + "(SELECT AVG(r.rating) FROM quiz_ratings r WHERE r.quiz_id = q.quiz_id) AS average_rating, "
                    + "(SELECT COUNT(*) FROM quiz_ratings r WHERE r.quiz_id = q.quiz_id) AS rating_count "
                    + "FROM quizzes q JOIN users u ON u.user_id = q.owner_id ";

    private static final String HAS_QUESTIONS =
            "EXISTS (SELECT 1 FROM questions qs WHERE qs.quiz_id = q.quiz_id)";

    private static final String NEWEST_FIRST = " ORDER BY q.creation_date DESC, q.quiz_id DESC";

    /**
     * Who may see a quiz, as SQL, for every query that lists quizzes or attempts at quizzes: public
     * quizzes, the viewer's own quizzes, and "friends only" quizzes of the viewer's friends. The
     * quizzes table must be called "q", and the viewer's user id goes into both "?".
     * Admins get no exception here, so their lists look like everyone else's; they reach any quiz
     * through the admin page (see QuizAccess).
     */
    static final String VISIBLE_TO =
            "(q.visibility = 'public' OR q.owner_id = ? OR (q.visibility = 'friends' AND EXISTS ("
                    + "SELECT 1 FROM friends vf WHERE vf.user_id = q.owner_id AND vf.friend_id = ? "
                    + "AND vf.status = 'accepted')))";

    /** Inserts a quiz and returns its new id. */
    public int create(Quiz quiz) {
        int id = Jdbc.insert("INSERT INTO quizzes (owner_id, title, description, random_order, "
                        + "multiple_pages, immediate_correction, visibility, time_limit_seconds) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                quiz.getOwnerId(), quiz.getTitle(), quiz.getDescription(), quiz.isRandomOrder(),
                quiz.isMultiplePages(), quiz.isImmediateCorrection(), quiz.getVisibility().getDbValue(),
                quiz.getTimeLimitSeconds());
        quiz.setQuizId(id);
        return id;
    }

    /** Saves a quiz's title, description and settings (not its owner or questions). */
    public boolean update(Quiz quiz) {
        return Jdbc.update("UPDATE quizzes SET title = ?, description = ?, random_order = ?, multiple_pages = ?, "
                        + "immediate_correction = ?, visibility = ?, time_limit_seconds = ? WHERE quiz_id = ?",
                quiz.getTitle(), quiz.getDescription(), quiz.isRandomOrder(), quiz.isMultiplePages(),
                quiz.isImmediateCorrection(), quiz.getVisibility().getDbValue(), quiz.getTimeLimitSeconds(),
                quiz.getQuizId()) > 0;
    }

    public Quiz findById(int quizId) {
        return Jdbc.queryOne(SELECT_QUIZ + "WHERE q.quiz_id = ?", QuizDAO::map, quizId);
    }

    /** Every quiz, whoever may see it. Only for the admin page. */
    public List<Quiz> findAll() {
        return Jdbc.query(SELECT_QUIZ + NEWEST_FIRST, QuizDAO::map);
    }

    /** Every quiz the viewer may see, newest first (for the leaderboard's quiz filter). */
    public List<Quiz> findVisibleTo(int viewerId) {
        return Jdbc.query(SELECT_QUIZ + "WHERE " + VISIBLE_TO + NEWEST_FIRST, QuizDAO::map, viewerId, viewerId);
    }

    /**
     * One page of the quizzes the viewer may see, in the chosen order. With a non-empty
     * {@code text}, only quizzes whose title or description contains it.
     */
    public List<Quiz> findVisible(int viewerId, String text, QuizSort sort, int limit, int offset) {
        String orderBy = switch (sort) {
            case POPULAR -> " ORDER BY attempt_count DESC, q.creation_date DESC, q.quiz_id DESC";
            // Quizzes nobody rated yet have no average (NULL), which sorts after every number.
            case RATED -> " ORDER BY average_rating DESC, rating_count DESC, q.creation_date DESC, q.quiz_id DESC";
            case NEWEST -> NEWEST_FIRST;
        };
        List<Object> params = visibleParams(viewerId, text);
        params.add(limit);
        params.add(offset);
        return Jdbc.query(SELECT_QUIZ + visibleWhere(text) + orderBy + " LIMIT ? OFFSET ?",
                QuizDAO::map, params.toArray());
    }

    /** How many quizzes {@link #findVisible} can show in total. */
    public int countVisible(int viewerId, String text) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quizzes q " + visibleWhere(text),
                visibleParams(viewerId, text).toArray());
    }

    private static String visibleWhere(String text) {
        return "WHERE " + VISIBLE_TO + (text.isEmpty() ? "" : " AND (q.title LIKE ? OR q.description LIKE ?)");
    }

    private static List<Object> visibleParams(int viewerId, String text) {
        List<Object> params = new ArrayList<>(List.of(viewerId, viewerId));
        if (!text.isEmpty()) {
            String pattern = Jdbc.likePattern(text);
            params.add(pattern);
            params.add(pattern);
        }
        return params;
    }

    /** The quizzes someone created that the viewer may see (all of them if they are the viewer's own). */
    public List<Quiz> findByOwner(int ownerId, int viewerId) {
        return Jdbc.query(SELECT_QUIZ + "WHERE q.owner_id = ? AND " + VISIBLE_TO + NEWEST_FIRST,
                QuizDAO::map, ownerId, viewerId, viewerId);
    }

    /** Playable quizzes with the most attempts, among those the viewer may see. */
    public List<Quiz> findPopular(int viewerId, int limit) {
        return Jdbc.query(SELECT_QUIZ + "WHERE " + HAS_QUESTIONS + " AND " + VISIBLE_TO
                        + " ORDER BY attempt_count DESC, q.creation_date DESC, q.quiz_id DESC LIMIT ?",
                QuizDAO::map, viewerId, viewerId, limit);
    }

    /** The newest playable quizzes the viewer may see. */
    public List<Quiz> findRecent(int viewerId, int limit) {
        return Jdbc.query(SELECT_QUIZ + "WHERE " + HAS_QUESTIONS + " AND " + VISIBLE_TO + NEWEST_FIRST + " LIMIT ?",
                QuizDAO::map, viewerId, viewerId, limit);
    }

    public int count() {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quizzes");
    }

    public int countByOwner(int ownerId) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM quizzes WHERE owner_id = ?", ownerId);
    }

    /** Deletes a quiz; its questions, attempts and challenges go with it (ON DELETE CASCADE). */
    public boolean delete(int quizId) {
        return Jdbc.update("DELETE FROM quizzes WHERE quiz_id = ?", quizId) > 0;
    }

    /** Admin tool: removes every attempt of a quiz. Returns how many attempts were deleted. */
    public int clearHistory(int quizId) {
        return Jdbc.update("DELETE FROM quiz_attempts WHERE quiz_id = ?", quizId);
    }

    private static Quiz map(ResultSet rs) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setQuizId(rs.getInt("quiz_id"));
        quiz.setOwnerId(rs.getInt("owner_id"));
        quiz.setOwnerUsername(rs.getString("owner_username"));
        quiz.setTitle(rs.getString("title"));
        quiz.setDescription(rs.getString("description"));
        quiz.setRandomOrder(rs.getBoolean("random_order"));
        quiz.setMultiplePages(rs.getBoolean("multiple_pages"));
        quiz.setImmediateCorrection(rs.getBoolean("immediate_correction"));
        quiz.setVisibility(QuizVisibility.fromDbValue(rs.getString("visibility")));
        quiz.setTimeLimitSeconds(Jdbc.nullableInt(rs, "time_limit_seconds"));
        quiz.setCreationDate(rs.getTimestamp("creation_date"));
        quiz.setQuestionCount(rs.getInt("question_count"));
        quiz.setAttemptCount(rs.getInt("attempt_count"));
        double averageRating = rs.getDouble("average_rating");
        quiz.setAverageRating(rs.wasNull() ? null : averageRating);
        quiz.setRatingCount(rs.getInt("rating_count"));
        return quiz;
    }
}
