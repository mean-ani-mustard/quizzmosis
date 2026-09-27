package com.quizzmosis.dao;

import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuestionType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes questions together with their choices ({@code question_choices}) and
 * accepted answers ({@code question_answers}).
 */
public class QuestionDAO {

    private static final String SELECT_QUESTION =
            "SELECT q.question_id, q.quiz_id, q.question_type, q.question_text, q.image_url, q.question_order "
                    + "FROM questions q ";

    /**
     * Adds a question with its choices or accepted answers in one transaction, so a question can
     * never be saved without its answers. The question goes to the end of the quiz.
     *
     * @return the new question's id
     */
    public int add(Question question) {
        return Jdbc.inTransaction(connection -> {
            int order = Jdbc.queryInt(connection,
                    "SELECT COALESCE(MAX(question_order), 0) + 1 FROM questions WHERE quiz_id = ?",
                    question.getQuizId());
            int questionId = Jdbc.insert(connection,
                    "INSERT INTO questions (quiz_id, question_type, question_text, image_url, question_order) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    question.getQuizId(), question.getType().getDbValue(), question.getQuestionText(),
                    question.getImageUrl(), order);
            question.setQuestionId(questionId);
            question.setQuestionOrder(order);
            insertChoicesOrAnswers(connection, question);
            return questionId;
        });
    }

    /**
     * Saves an edited question. Its old choices or accepted answers are replaced by the new ones,
     * all in one transaction. Earlier results don't change: they keep the answer texts they were
     * graded with.
     */
    public void update(Question question) {
        Jdbc.inTransaction(connection -> {
            int id = question.getQuestionId();
            Jdbc.update(connection, "UPDATE questions SET question_type = ?, question_text = ?, image_url = ? "
                            + "WHERE question_id = ?",
                    question.getType().getDbValue(), question.getQuestionText(), question.getImageUrl(), id);
            Jdbc.update(connection, "DELETE FROM question_choices WHERE question_id = ?", id);
            Jdbc.update(connection, "DELETE FROM question_answers WHERE question_id = ?", id);
            insertChoicesOrAnswers(connection, question);
            return null;
        });
    }

    private static void insertChoicesOrAnswers(Connection connection, Question question) throws SQLException {
        if (question.isMultipleChoice()) {
            for (Choice choice : question.getChoices()) {
                Jdbc.update(connection,
                        "INSERT INTO question_choices (question_id, choice_text, is_correct) VALUES (?, ?, ?)",
                        question.getQuestionId(), choice.getChoiceText(), choice.isCorrect());
            }
        } else {
            for (String answer : question.getAcceptedAnswers()) {
                Jdbc.update(connection, "INSERT INTO question_answers (question_id, answer_text) VALUES (?, ?)",
                        question.getQuestionId(), answer);
            }
        }
    }

    /** A single question with its choices/answers, or {@code null}. */
    public Question findById(int questionId) {
        List<Question> questions = load("q.question_id = ?", questionId);
        return questions.isEmpty() ? null : questions.get(0);
    }

    /** All questions of a quiz in their stored order, each with its choices/answers. */
    public List<Question> findByQuiz(int quizId) {
        return load("q.quiz_id = ?", quizId);
    }

    /** Deletes a question; its choices, answers and attempt answers go with it (ON DELETE CASCADE). */
    public boolean delete(int questionId) {
        return Jdbc.update("DELETE FROM questions WHERE question_id = ?", questionId) > 0;
    }

    /**
     * Loads the matching questions with three queries in total (questions, choices, answers)
     * instead of two extra queries per question.
     */
    private List<Question> load(String condition, int param) {
        List<Question> questions = Jdbc.query(
                SELECT_QUESTION + "WHERE " + condition + " ORDER BY q.question_order, q.question_id",
                QuestionDAO::map, param);
        if (questions.isEmpty()) {
            return questions;
        }

        Map<Integer, Question> byId = new HashMap<>();
        for (Question question : questions) {
            byId.put(question.getQuestionId(), question);
        }

        Jdbc.query("SELECT c.choice_id, c.question_id, c.choice_text, c.is_correct "
                                + "FROM question_choices c JOIN questions q ON q.question_id = c.question_id "
                                + "WHERE " + condition + " ORDER BY c.choice_id",
                        QuestionDAO::mapChoice, param)
                .forEach(choice -> byId.get(choice.getQuestionId()).getChoices().add(choice));

        Jdbc.query("SELECT a.question_id, a.answer_text "
                                + "FROM question_answers a JOIN questions q ON q.question_id = a.question_id "
                                + "WHERE " + condition + " ORDER BY a.answer_id",
                        rs -> Map.entry(rs.getInt("question_id"), rs.getString("answer_text")), param)
                .forEach(entry -> byId.get(entry.getKey()).getAcceptedAnswers().add(entry.getValue()));

        return questions;
    }

    private static Question map(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setQuestionId(rs.getInt("question_id"));
        question.setQuizId(rs.getInt("quiz_id"));
        question.setType(QuestionType.fromDbValue(rs.getString("question_type")));
        question.setQuestionText(rs.getString("question_text"));
        question.setImageUrl(rs.getString("image_url"));
        question.setQuestionOrder(rs.getInt("question_order"));
        return question;
    }

    private static Choice mapChoice(ResultSet rs) throws SQLException {
        Choice choice = new Choice();
        choice.setChoiceId(rs.getInt("choice_id"));
        choice.setQuestionId(rs.getInt("question_id"));
        choice.setChoiceText(rs.getString("choice_text"));
        choice.setCorrect(rs.getBoolean("is_correct"));
        return choice;
    }
}
