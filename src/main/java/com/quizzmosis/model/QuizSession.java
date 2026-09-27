package com.quizzmosis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The quiz a user is currently taking.
 *
 * <p>It lives in the HTTP session between requests and is only written to the database once the
 * quiz is finished. That way an abandoned quiz never leaves a half-finished attempt with a score
 * of 0 behind in the history, the leaderboards or the statistics.</p>
 */
public class QuizSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Answers that arrive up to this long after the time limit still count, because the page's
     * own "time's up" submit needs a moment to reach the server.
     */
    private static final long GRACE_MILLIS = 5_000;

    private final int quizId;
    private final String quizTitle;
    private final boolean multiplePages;
    private final boolean immediateCorrection;
    private final List<Question> questions;
    /** questionId -> graded answer, in the order the answers were given. */
    private final Map<Integer, AttemptAnswer> answers = new LinkedHashMap<>();
    private final long startedAtMillis;
    /** How long the player has, or {@code null} if the quiz has no time limit. */
    private final Integer timeLimitSeconds;
    private AttemptAnswer pendingFeedback;
    private boolean finished;

    public QuizSession(Quiz quiz, List<Question> questionsInDisplayOrder, long startedAtMillis) {
        if (questionsInDisplayOrder.isEmpty()) {
            throw new IllegalArgumentException("A quiz needs at least one question to be taken");
        }
        this.quizId = quiz.getQuizId();
        this.quizTitle = quiz.getTitle();
        this.multiplePages = quiz.isMultiplePages();
        // Immediate correction only makes sense when the questions are shown one at a time.
        this.immediateCorrection = quiz.isMultiplePages() && quiz.isImmediateCorrection();
        this.questions = new ArrayList<>(questionsInDisplayOrder);
        this.startedAtMillis = startedAtMillis;
        this.timeLimitSeconds = quiz.getTimeLimitSeconds();
    }

    public int getQuizId() {
        return quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public boolean isMultiplePages() {
        return multiplePages;
    }

    public boolean isImmediateCorrection() {
        return immediateCorrection;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public int getAnsweredCount() {
        return answers.size();
    }

    public boolean isComplete() {
        return answers.size() == questions.size();
    }

    /** The first question that hasn't been answered yet, or {@code null} once all are answered. */
    public Question getCurrentQuestion() {
        for (Question question : questions) {
            if (!answers.containsKey(question.getQuestionId())) {
                return question;
            }
        }
        return null;
    }

    /** 1-based number of the current question, for "Question 2 of 5". */
    public int getCurrentNumber() {
        return Math.min(answers.size() + 1, questions.size());
    }

    /** Finds a question of <em>this</em> quiz, or returns {@code null} (e.g. for an id from another quiz). */
    public Question findQuestion(int questionId) {
        for (Question question : questions) {
            if (question.getQuestionId() == questionId) {
                return question;
            }
        }
        return null;
    }

    /**
     * Stores a graded answer. Every question can be answered only once, so resubmitting a form
     * (double click, back button) can't change the score.
     *
     * @return {@code false} if the question isn't part of this quiz or was already answered
     */
    public synchronized boolean recordAnswer(AttemptAnswer answer) {
        int questionId = answer.getQuestionId();
        if (findQuestion(questionId) == null || answers.containsKey(questionId)) {
            return false;
        }
        answers.put(questionId, answer);
        if (immediateCorrection) {
            pendingFeedback = answer;
        }
        return true;
    }

    /**
     * In immediate-correction mode: returns the answer the user should now see feedback for, and
     * forgets it so the feedback is shown exactly once. Returns {@code null} otherwise.
     */
    public synchronized AttemptAnswer takePendingFeedback() {
        AttemptAnswer feedback = pendingFeedback;
        pendingFeedback = null;
        return feedback;
    }

    public int getScore() {
        return (int) answers.values().stream().filter(AttemptAnswer::isCorrect).count();
    }

    /**
     * Every question in display order with its answer. Questions the user never answered are
     * included as wrong, empty answers, so the score is always out of the whole quiz.
     */
    public List<AttemptAnswer> getAnswersForAllQuestions() {
        List<AttemptAnswer> result = new ArrayList<>();
        for (Question question : questions) {
            AttemptAnswer answer = answers.get(question.getQuestionId());
            if (answer == null) {
                answer = new AttemptAnswer();
                answer.setQuestionId(question.getQuestionId());
                answer.setQuestionText(question.getQuestionText());
                answer.setCorrectAnswer(question.getCorrectAnswerText());
                answer.setCorrect(false);
            }
            result.add(answer);
        }
        return result;
    }

    /** Seconds since the start; with a time limit, never more than the limit. */
    public int getElapsedSeconds(long nowMillis) {
        int elapsed = (int) Math.max(0, (nowMillis - startedAtMillis) / 1000);
        return timeLimitSeconds == null ? elapsed : Math.min(elapsed, timeLimitSeconds);
    }

    /** Whole seconds left before the time limit, or {@code null} if the quiz has no time limit. */
    public Integer getSecondsLeft(long nowMillis) {
        if (timeLimitSeconds == null) {
            return null;
        }
        long leftMillis = deadlineMillis() - nowMillis;
        return (int) Math.max(0, (leftMillis + 999) / 1000);
    }

    /** Whether the time limit has passed. */
    public boolean isTimeUp(long nowMillis) {
        return timeLimitSeconds != null && nowMillis >= deadlineMillis();
    }

    /** Whether it's too late for answers to count: the time limit and the few seconds of grace are over. */
    public boolean isTooLate(long nowMillis) {
        return timeLimitSeconds != null && nowMillis > deadlineMillis() + GRACE_MILLIS;
    }

    private long deadlineMillis() {
        return startedAtMillis + timeLimitSeconds * 1000L;
    }

    /**
     * Marks the quiz as finished. Returns {@code false} if that already happened, so that a
     * double-clicked "Finish" button can't save the same attempt twice.
     */
    public synchronized boolean tryMarkFinished() {
        if (finished) {
            return false;
        }
        finished = true;
        return true;
    }
}
