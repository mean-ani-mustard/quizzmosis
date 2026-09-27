package com.quizzmosis.model;

import java.io.Serializable;

/**
 * The user's answer to one question of an attempt, already graded.
 *
 * <p>{@code correctAnswer} is a copy of the correct answer text at the time of grading, so the
 * results page can show it even if the quiz author edits the question later.</p>
 */
public class AttemptAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int attemptId;
    private int questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private boolean correct;

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    /** What the user typed or picked; {@code null} when the question was left unanswered. */
    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
