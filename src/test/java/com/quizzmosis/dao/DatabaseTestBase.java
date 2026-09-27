package com.quizzmosis.dao;

import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuestionType;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizAttempt;
import com.quizzmosis.model.QuizVisibility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for the integration tests (the classes ending in "IT"), which run against a real
 * MySQL database. {@code mvn verify} runs them; pom.xml points them at the {@code quiz_db_test}
 * database, and the README explains how to start one.
 *
 * <p>These tests delete data, so as a safety net they refuse to run against any database whose
 * name doesn't end in {@code _test}. db/schema.sql is loaded once per test run, and every test
 * starts with empty tables (except the fixed list of achievements).</p>
 */
public abstract class DatabaseTestBase {

    /** Children before parents, so the foreign keys never block a delete. */
    private static final List<String> TABLES = List.of(
            "user_achievements", "announcements", "quiz_ratings", "quiz_comments", "messages", "challenges", "friends",
            "attempt_answers", "quiz_attempts", "question_answers", "question_choices", "questions", "quizzes", "users");

    private static boolean schemaLoaded;

    protected final UserDAO userDAO = new UserDAO();
    protected final QuizDAO quizDAO = new QuizDAO();
    protected final QuestionDAO questionDAO = new QuestionDAO();
    protected final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();

    @BeforeAll
    protected static void loadSchemaOnce() throws IOException, SQLException {
        String databaseName = databaseName(Database.url());
        if (!databaseName.endsWith("_test")) {
            throw new IllegalStateException("Integration tests delete data, so they only run against a "
                    + "database whose name ends in _test, not '" + databaseName + "'. See the README.");
        }
        synchronized (DatabaseTestBase.class) {
            if (!schemaLoaded) {
                runScript(Path.of("db", "schema.sql"));
                schemaLoaded = true;
            }
        }
    }

    @BeforeEach
    protected void emptyAllTables() {
        for (String table : TABLES) {
            execute("DELETE FROM " + table);
        }
    }

    // ----- test data -----

    /** A user whose display name equals the username. The password hash is a dummy value. */
    protected int createUser(String username) {
        return userDAO.create(username, "not-a-real-hash", username);
    }

    /** Makes two users friends: one sends a request and the other accepts it. */
    protected static void makeFriends(int userId, int otherId) {
        FriendsDAO friends = new FriendsDAO();
        friends.sendRequest(userId, otherId);
        friends.acceptRequest(otherId, userId);
    }

    /** Changes who can see a quiz, the way the "edit quiz" page does. */
    protected void setVisibility(int quizId, QuizVisibility visibility) {
        Quiz quiz = quizDAO.findById(quizId);
        quiz.setVisibility(visibility);
        quizDAO.update(quiz);
    }

    protected int createQuiz(int ownerId, String title) {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(ownerId);
        quiz.setTitle(title);
        quiz.setDescription("All about " + title);
        return quizDAO.create(quiz);
    }

    protected Question addTypedQuestion(int quizId, String text, String... acceptedAnswers) {
        Question question = new Question();
        question.setQuizId(quizId);
        question.setType(QuestionType.QUESTION_RESPONSE);
        question.setQuestionText(text);
        question.setAcceptedAnswers(List.of(acceptedAnswers));
        questionDAO.add(question);
        return question;
    }

    /** The first option is the correct one. */
    protected Question addMultipleChoiceQuestion(int quizId, String text, String correct, String... wrong) {
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(correct, true));
        for (String option : wrong) {
            choices.add(new Choice(option, false));
        }
        Question question = new Question();
        question.setQuizId(quizId);
        question.setType(QuestionType.MULTIPLE_CHOICE);
        question.setQuestionText(text);
        question.setChoices(choices);
        questionDAO.add(question);
        // Reload, so the choices have the ids the database gave them.
        return questionDAO.findById(question.getQuestionId());
    }

    /** Saves a finished attempt (without per-question answers) and returns its id. */
    protected int saveAttempt(int userId, int quizId, int score, int totalQuestions, int seconds) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quizId);
        attempt.setScore(score);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setDurationSeconds(seconds);
        return attemptDAO.saveFinishedAttempt(attempt, List.of());
    }

    /** Moves an attempt into the past, e.g. to test the leaderboard's time filters. */
    protected void backdateAttempt(int attemptId, int days) {
        execute("UPDATE quiz_attempts SET attempt_date = NOW() - INTERVAL " + days + " DAY WHERE attempt_id = ?",
                attemptId);
    }

    // ----- plain SQL -----

    protected static void execute(String sql, Object... params) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Test SQL failed: " + sql, e);
        }
    }

    protected static int queryInt(String sql, Object... params) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Test SQL failed: " + sql, e);
        }
    }

    /** "jdbc:mysql://localhost:3306/quiz_db_test?x=y" -> "quiz_db_test" */
    static String databaseName(String jdbcUrl) {
        String withoutParameters = jdbcUrl.split("\\?")[0];
        return withoutParameters.substring(withoutParameters.lastIndexOf('/') + 1);
    }

    /**
     * Runs an SQL file. Good enough for schema.sql: every statement ends with ';' at the end of a
     * line, and comment lines start with "--".
     */
    private static void runScript(Path file) throws IOException, SQLException {
        if (!Files.exists(file)) {
            throw new IllegalStateException(file.toAbsolutePath() + " not found; run the tests from the project folder");
        }
        try (Connection connection = Database.getConnection(); Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                sql.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statement.execute(sql.substring(0, sql.lastIndexOf(";")));
                    sql.setLength(0);
                }
            }
        }
    }
}
