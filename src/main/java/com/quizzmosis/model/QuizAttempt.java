package com.quizzmosis.model;

import com.quizzmosis.util.Format;

import java.sql.Timestamp;

/** A finished attempt at a quiz (one row of {@code quiz_attempts}). */
public class QuizAttempt {

    private int attemptId;
    private int userId;
    private int quizId;
    private int score;
    private int totalQuestions;
    private int durationSeconds;
    private Timestamp attemptDate;

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Timestamp getAttemptDate() {
        return attemptDate;
    }

    public void setAttemptDate(Timestamp attemptDate) {
        this.attemptDate = attemptDate;
    }

    public int getPercentage() {
        return Format.percent(score, totalQuestions);
    }

    public String getFormattedDuration() {
        return Format.duration(durationSeconds);
    }
}
