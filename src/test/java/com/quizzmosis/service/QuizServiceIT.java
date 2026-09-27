package com.quizzmosis.service;

import com.quizzmosis.dao.DatabaseTestBase;
import com.quizzmosis.model.Achievement;
import com.quizzmosis.model.AttemptAnswer;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizAttempt;
import com.quizzmosis.model.QuizSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Taking a quiz from start to finish, the way TakeQuizServlet does it. */
class QuizServiceIT extends DatabaseTestBase {

    private final QuizService quizService = new QuizService();
    private int alice;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
    }

    @Test
    void finishingAQuizSavesTheAttemptWithEveryAnswer() {
        int quizId = createQuiz(alice, "Mixed quiz");
        Question france = addTypedQuestion(quizId, "Capital of France?", "Paris");
        Question sum = addMultipleChoiceQuestion(quizId, "2 + 2?", "4", "5");
        addTypedQuestion(quizId, "Capital of Japan?", "Tokyo");

        QuizSession session = quizService.start(quizDAO.findById(quizId), 0);
        session.recordAnswer(QuizGrader.grade(france, " paris "));
        String wrongChoice = String.valueOf(sum.getChoices().get(1).getChoiceId());
        session.recordAnswer(QuizGrader.grade(sum, wrongChoice));
        // The third question is never answered.

        QuizService.FinishedQuiz finished = quizService.finish(alice, session, 42_000);

        QuizAttempt attempt = attemptDAO.findById(finished.attemptId());
        assertEquals(1, attempt.getScore());
        assertEquals(3, attempt.getTotalQuestions());
        assertEquals(42, attempt.getDurationSeconds());

        List<AttemptAnswer> answers = attemptDAO.findAnswers(finished.attemptId());
        assertEquals(Arrays.asList("paris", "5", null), answers.stream().map(AttemptAnswer::getUserAnswer).toList());
        assertEquals(List.of(true, false, false), answers.stream().map(AttemptAnswer::isCorrect).toList());
        assertEquals("Tokyo", answers.get(2).getCorrectAnswer());
        assertEquals("Capital of Japan?", answers.get(2).getQuestionText());

        List<String> earned = finished.newAchievements().stream().map(Achievement::getName).toList();
        assertEquals(List.of("I am the Greatest"), earned, "the first result is also the best one");
    }

    @Test
    void randomOrderShufflesButKeepsEveryQuestionOnce() {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(alice);
        quiz.setTitle("Shuffled");
        quiz.setRandomOrder(true);
        int quizId = quizDAO.create(quiz);
        for (int i = 1; i <= 8; i++) {
            addTypedQuestion(quizId, "Question " + i, "answer");
        }

        QuizSession session = quizService.start(quizDAO.findById(quizId), 0);

        List<Integer> shown = session.getQuestions().stream().map(Question::getQuestionId).sorted().toList();
        List<Integer> stored = questionDAO.findByQuiz(quizId).stream().map(Question::getQuestionId).sorted().toList();
        assertEquals(stored, shown);
    }
}
