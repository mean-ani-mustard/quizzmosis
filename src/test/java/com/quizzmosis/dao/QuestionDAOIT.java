package com.quizzmosis.dao;

import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDAOIT extends DatabaseTestBase {

    private int quiz;

    @BeforeEach
    void setUpQuiz() {
        quiz = createQuiz(createUser("alice"), "Mixed quiz");
    }

    @Test
    void everyQuestionTypeIsStoredWithItsAnswers() {
        addTypedQuestion(quiz, "Who was the first US president?", "George Washington", "Washington");
        addMultipleChoiceQuestion(quiz, "What is 2 + 2?", "4", "3", "5");

        Question blank = new Question();
        blank.setQuizId(quiz);
        blank.setType(QuestionType.FILL_BLANK);
        blank.setQuestionText("Water boils at ____ degrees Celsius.");
        blank.setAcceptedAnswers(List.of("100"));
        questionDAO.add(blank);

        Question picture = new Question();
        picture.setQuizId(quiz);
        picture.setType(QuestionType.PICTURE_RESPONSE);
        picture.setQuestionText("Which animal is this?");
        picture.setImageUrl("https://example.com/cat.jpg");
        picture.setAcceptedAnswers(List.of("cat", "kitten"));
        questionDAO.add(picture);

        List<Question> questions = questionDAO.findByQuiz(quiz);

        assertEquals(4, questions.size());
        assertEquals(List.of(1, 2, 3, 4), questions.stream().map(Question::getQuestionOrder).toList());

        Question typed = questions.get(0);
        assertEquals(QuestionType.QUESTION_RESPONSE, typed.getType());
        assertEquals(List.of("George Washington", "Washington"), typed.getAcceptedAnswers());
        assertTrue(typed.getChoices().isEmpty());

        Question choice = questions.get(1);
        assertEquals(QuestionType.MULTIPLE_CHOICE, choice.getType());
        assertEquals(List.of("4", "3", "5"), choice.getChoices().stream().map(Choice::getChoiceText).toList());
        assertEquals(List.of(true, false, false), choice.getChoices().stream().map(Choice::isCorrect).toList());
        assertTrue(choice.getAcceptedAnswers().isEmpty());

        assertEquals(QuestionType.FILL_BLANK, questions.get(2).getType());
        assertEquals("https://example.com/cat.jpg", questions.get(3).getImageUrl());
        assertEquals(List.of("cat", "kitten"), questions.get(3).getAcceptedAnswers());
    }

    @Test
    void textInAnyLanguageSurvivesTheRoundTrip() {
        Question question = addTypedQuestion(quiz, "Capital of Brazil? 🇧🇷", "Brasília");

        Question stored = questionDAO.findById(question.getQuestionId());

        assertEquals("Capital of Brazil? 🇧🇷", stored.getQuestionText());
        assertEquals(List.of("Brasília"), stored.getAcceptedAnswers());
    }

    @Test
    void findByIdLoadsOnlyThatQuestion() {
        addTypedQuestion(quiz, "First?", "1");
        Question second = addMultipleChoiceQuestion(quiz, "Second?", "yes", "no");

        Question found = questionDAO.findById(second.getQuestionId());

        assertEquals("Second?", found.getQuestionText());
        assertEquals(2, found.getChoices().size());
        assertNull(questionDAO.findById(999_999));
    }

    @Test
    void deletingAQuestionDeletesItsChoicesAndKeepsTheRest() {
        Question first = addMultipleChoiceQuestion(quiz, "Delete me", "a", "b");
        addTypedQuestion(quiz, "Keep me", "yes");

        assertTrue(questionDAO.delete(first.getQuestionId()));

        assertEquals(List.of("Keep me"), questionDAO.findByQuiz(quiz).stream().map(Question::getQuestionText).toList());
        assertEquals(0, queryInt("SELECT COUNT(*) FROM question_choices"));
        assertFalse(questionDAO.delete(first.getQuestionId()), "already gone");
    }

    @Test
    void newQuestionsAlwaysGoToTheEnd() {
        Question first = addTypedQuestion(quiz, "First", "1");
        addTypedQuestion(quiz, "Second", "2");
        questionDAO.delete(first.getQuestionId());

        Question third = addTypedQuestion(quiz, "Third", "3");

        assertEquals(3, third.getQuestionOrder());
        assertEquals(List.of("Second", "Third"),
                questionDAO.findByQuiz(quiz).stream().map(Question::getQuestionText).toList());
    }
    @Test
    void editingAQuestionReplacesItsChoicesOrAnswers() {
        Question question = addMultipleChoiceQuestion(quiz, "What is 2 + 2?", "4", "3", "5");
        int attempt = saveAttempt(createUser("bob"), quiz, 1, 1, 10);
        execute("INSERT INTO attempt_answers (attempt_id, question_id, user_answer_text, correct_answer, is_correct) "
                + "VALUES (?, ?, '4', '4', TRUE)", attempt, question.getQuestionId());

        question.setQuestionText("What is 3 + 3?");
        question.setChoices(List.of(new Choice("6", true), new Choice("9", false)));
        questionDAO.update(question);

        Question edited = questionDAO.findById(question.getQuestionId());
        assertEquals("What is 3 + 3?", edited.getQuestionText());
        assertEquals(List.of("6", "9"), edited.getChoices().stream().map(Choice::getChoiceText).toList());
        assertEquals(1, edited.getQuestionOrder(), "it keeps its place in the quiz");
        assertEquals(1, queryInt("SELECT COUNT(*) FROM attempt_answers WHERE question_id = ?", question.getQuestionId()),
                "earlier results stay");

        // A question can even change its type: the old options go, accepted answers come.
        edited.setType(QuestionType.QUESTION_RESPONSE);
        edited.setAcceptedAnswers(List.of("six", "6"));
        questionDAO.update(edited);

        Question typed = questionDAO.findById(question.getQuestionId());
        assertEquals(QuestionType.QUESTION_RESPONSE, typed.getType());
        assertTrue(typed.getChoices().isEmpty());
        assertEquals(List.of("six", "6"), typed.getAcceptedAnswers());
    }
}
