package com.quizzmosis.service;

import com.quizzmosis.model.AttemptAnswer;
import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizGraderTest {

    // ----- typed answers (question-response, fill in the blank, picture-response) -----

    @Test
    void exactAnswerIsCorrect() {
        AttemptAnswer answer = QuizGrader.grade(presidentQuestion(), "George Washington");

        assertTrue(answer.isCorrect());
        assertEquals("George Washington", answer.getUserAnswer());
    }

    @Test
    void caseAndExtraSpacesDoNotMatter() {
        assertTrue(QuizGrader.grade(presidentQuestion(), "  george   WASHINGTON ").isCorrect());
        assertTrue(QuizGrader.grade(presidentQuestion(), "george\twashington").isCorrect());
    }

    @Test
    void anyOfTheAcceptedAnswersIsCorrect() {
        assertTrue(QuizGrader.grade(presidentQuestion(), "washington").isCorrect());
    }

    @Test
    void wrongOrPartialAnswerIsIncorrect() {
        AttemptAnswer answer = QuizGrader.grade(presidentQuestion(), " Abraham Lincoln ");

        assertFalse(answer.isCorrect());
        assertEquals("Abraham Lincoln", answer.getUserAnswer(), "stored trimmed");
        assertFalse(QuizGrader.grade(presidentQuestion(), "George").isCorrect(), "no partial matches");
    }

    @Test
    void emptyAnswerIsIncorrectAndStoredAsNoAnswer() {
        for (String response : new String[] {null, "", "   "}) {
            AttemptAnswer answer = QuizGrader.grade(presidentQuestion(), response);
            assertFalse(answer.isCorrect());
            assertNull(answer.getUserAnswer());
        }
    }

    @Test
    void resultShowsTheQuestionAndAllAcceptedAnswers() {
        AttemptAnswer answer = QuizGrader.grade(presidentQuestion(), "Lincoln");

        assertEquals(1, answer.getQuestionId());
        assertEquals("Who was the first president of the United States?", answer.getQuestionText());
        assertEquals("George Washington / Washington", answer.getCorrectAnswer());
    }

    @Test
    void veryLongAnswersAreCutToFitTheDatabaseColumn() {
        AttemptAnswer answer = QuizGrader.grade(presidentQuestion(), "x".repeat(2_000));

        assertEquals(500, answer.getUserAnswer().length());
    }

    @Test
    void fillInTheBlankAndPictureQuestionsAreGradedTheSameWay() {
        Question blank = typedQuestion(QuestionType.FILL_BLANK, "Water boils at ____ degrees Celsius.", "100");
        Question picture = typedQuestion(QuestionType.PICTURE_RESPONSE, "What animal is this?", "Cat");

        assertTrue(QuizGrader.grade(blank, " 100 ").isCorrect());
        assertTrue(QuizGrader.grade(picture, "cat").isCorrect());
        assertFalse(QuizGrader.grade(picture, "dog").isCorrect());
    }

    // ----- multiple choice: the response is the id of the chosen option -----

    @Test
    void choosingTheCorrectOptionIsCorrect() {
        AttemptAnswer answer = QuizGrader.grade(additionQuestion(), "11");

        assertTrue(answer.isCorrect());
        assertEquals("4", answer.getUserAnswer());
        assertEquals("4", answer.getCorrectAnswer());
    }

    @Test
    void choosingAWrongOptionIsIncorrect() {
        AttemptAnswer answer = QuizGrader.grade(additionQuestion(), "12");

        assertFalse(answer.isCorrect());
        assertEquals("5", answer.getUserAnswer());
    }

    @Test
    void optionOfAnotherQuestionDoesNotCount() {
        // Choice 99 might be a correct option of a different question; it must not score here.
        AttemptAnswer answer = QuizGrader.grade(additionQuestion(), "99");

        assertFalse(answer.isCorrect());
        assertNull(answer.getUserAnswer());
    }

    @Test
    void missingOrInvalidChoiceIsIncorrect() {
        assertFalse(QuizGrader.grade(additionQuestion(), null).isCorrect());
        assertFalse(QuizGrader.grade(additionQuestion(), "").isCorrect());
        assertFalse(QuizGrader.grade(additionQuestion(), "four").isCorrect());
    }

    @Test
    void normalizeLowercasesAndCollapsesWhitespace() {
        assertEquals("new york city", QuizGrader.normalize("  New\tYork   CITY \n"));
        assertEquals("", QuizGrader.normalize(null));
    }

    // ----- helpers -----

    private static Question presidentQuestion() {
        Question question = typedQuestion(QuestionType.QUESTION_RESPONSE,
                "Who was the first president of the United States?", "George Washington", "Washington");
        question.setQuestionId(1);
        return question;
    }

    private static Question typedQuestion(QuestionType type, String text, String... acceptedAnswers) {
        Question question = new Question();
        question.setType(type);
        question.setQuestionText(text);
        question.setAcceptedAnswers(List.of(acceptedAnswers));
        return question;
    }

    private static Question additionQuestion() {
        Question question = new Question();
        question.setQuestionId(2);
        question.setType(QuestionType.MULTIPLE_CHOICE);
        question.setQuestionText("What is 2 + 2?");
        question.setChoices(List.of(choice(10, "3", false), choice(11, "4", true), choice(12, "5", false)));
        return question;
    }

    private static Choice choice(int id, String text, boolean correct) {
        Choice choice = new Choice(text, correct);
        choice.setChoiceId(id);
        return choice;
    }
}
