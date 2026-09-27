package com.quizzmosis.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizSessionTest {

    private static final long START = 1_000_000L;

    @Test
    void quizWithoutQuestionsCannotBeStarted() {
        assertThrows(IllegalArgumentException.class, () -> new QuizSession(quiz(false, false), List.of(), START));
    }

    @Test
    void questionsAreAnsweredInOrder() {
        QuizSession session = newSession(true, false);

        assertEquals(1, session.getCurrentQuestion().getQuestionId());
        assertEquals(1, session.getCurrentNumber());

        session.recordAnswer(answer(1, true));

        assertEquals(2, session.getCurrentQuestion().getQuestionId());
        assertEquals(2, session.getCurrentNumber());
        assertEquals(1, session.getAnsweredCount());
        assertFalse(session.isComplete());
    }

    @Test
    void afterTheLastAnswerTheQuizIsComplete() {
        QuizSession session = newSession(true, false);
        session.recordAnswer(answer(1, true));
        session.recordAnswer(answer(2, false));
        session.recordAnswer(answer(3, true));

        assertTrue(session.isComplete());
        assertNull(session.getCurrentQuestion());
        assertEquals(3, session.getCurrentNumber(), "stays at the last question");
        assertEquals(2, session.getScore());
    }

    @Test
    void eachQuestionCanOnlyBeAnsweredOnce() {
        QuizSession session = newSession(true, false);

        assertTrue(session.recordAnswer(answer(1, false)));
        assertFalse(session.recordAnswer(answer(1, true)), "resubmitting must not change the answer");
        assertEquals(0, session.getScore());
    }

    @Test
    void answersToQuestionsOfOtherQuizzesAreIgnored() {
        QuizSession session = newSession(false, false);

        assertFalse(session.recordAnswer(answer(42, true)));
        assertEquals(0, session.getAnsweredCount());
        assertNull(session.findQuestion(42));
    }

    @Test
    void unansweredQuestionsCountAsWrong() {
        QuizSession session = newSession(false, false);
        session.recordAnswer(answer(2, true));

        List<AttemptAnswer> all = session.getAnswersForAllQuestions();

        assertEquals(3, all.size(), "one entry per question, in display order");
        assertEquals(1, all.get(0).getQuestionId());
        assertFalse(all.get(0).isCorrect());
        assertNull(all.get(0).getUserAnswer());
        assertEquals("Answer 1", all.get(0).getCorrectAnswer());
        assertTrue(all.get(1).isCorrect());
        assertFalse(all.get(2).isCorrect());
    }

    @Test
    void immediateFeedbackIsShownExactlyOnce() {
        QuizSession session = newSession(true, true);
        AttemptAnswer first = answer(1, false);
        session.recordAnswer(first);

        assertTrue(session.isImmediateCorrection());
        assertSame(first, session.takePendingFeedback());
        assertNull(session.takePendingFeedback(), "a page refresh must not show it again");
    }

    @Test
    void immediateCorrectionNeedsOneQuestionPerPage() {
        QuizSession session = newSession(false, true);
        session.recordAnswer(answer(1, true));

        assertFalse(session.isImmediateCorrection());
        assertNull(session.takePendingFeedback());
    }

    @Test
    void quizCanOnlyBeFinishedOnce() {
        QuizSession session = newSession(false, false);

        assertTrue(session.tryMarkFinished());
        assertFalse(session.tryMarkFinished(), "a double click on Finish must not save twice");
    }

    @Test
    void elapsedTimeIsMeasuredInWholeSeconds() {
        QuizSession session = newSession(false, false);

        assertEquals(0, session.getElapsedSeconds(START + 999));
        assertEquals(75, session.getElapsedSeconds(START + 75_400));
        assertEquals(0, session.getElapsedSeconds(START - 5_000), "never negative");
    }

    @Test
    void quizDetailsAreCopiedFromTheQuiz() {
        QuizSession session = newSession(true, false);

        assertEquals(7, session.getQuizId());
        assertEquals("Test quiz", session.getQuizTitle());
        assertTrue(session.isMultiplePages());
        assertEquals(3, session.getTotalQuestions());
    }

    // ----- helpers -----

    @Test
    void withoutATimeLimitTimeIsNeverUp() {
        QuizSession session = newSession(false, false);

        assertNull(session.getSecondsLeft(START + 3_600_000));
        assertFalse(session.isTimeUp(START + 3_600_000));
        assertFalse(session.isTooLate(START + 3_600_000));
    }

    @Test
    void timeLimitCountsDownAndEndsTheQuiz() {
        Quiz quiz = quiz(false, false);
        quiz.setTimeLimitSeconds(60);
        QuizSession session = new QuizSession(quiz, List.of(question(1)), START);

        assertEquals(60, session.getSecondsLeft(START));
        assertEquals(30, session.getSecondsLeft(START + 30_500), "rounded up, so 0 only shows when time is up");
        assertFalse(session.isTimeUp(START + 59_999));
        assertTrue(session.isTimeUp(START + 60_000));
        assertEquals(0, session.getSecondsLeft(START + 70_000));
        assertFalse(session.isTooLate(START + 63_000), "answers sent a moment after the limit still count");
        assertTrue(session.isTooLate(START + 66_000));
        assertEquals(60, session.getElapsedSeconds(START + 66_000), "the time taken never exceeds the limit");
    }

    private static QuizSession newSession(boolean multiplePages, boolean immediateCorrection) {
        List<Question> questions = List.of(question(1), question(2), question(3));
        return new QuizSession(quiz(multiplePages, immediateCorrection), questions, START);
    }

    private static Quiz quiz(boolean multiplePages, boolean immediateCorrection) {
        Quiz quiz = new Quiz();
        quiz.setQuizId(7);
        quiz.setTitle("Test quiz");
        quiz.setMultiplePages(multiplePages);
        quiz.setImmediateCorrection(immediateCorrection);
        return quiz;
    }

    private static Question question(int id) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setType(QuestionType.QUESTION_RESPONSE);
        question.setQuestionText("Question " + id);
        question.setAcceptedAnswers(List.of("Answer " + id));
        return question;
    }

    private static AttemptAnswer answer(int questionId, boolean correct) {
        AttemptAnswer answer = new AttemptAnswer();
        answer.setQuestionId(questionId);
        answer.setUserAnswer(correct ? "Answer " + questionId : "wrong");
        answer.setCorrect(correct);
        return answer;
    }
}
