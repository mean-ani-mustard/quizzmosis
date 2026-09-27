package com.quizzmosis.model;

import com.quizzmosis.util.Format;

import java.sql.Timestamp;

/**
 * One attempt together with the names of the user and the quiz. Used everywhere attempts are
 * listed: history, friends' activity, leaderboards and the quiz statistics tables.
 */
public class AttemptSummary {

    private int attemptId;
    private int userId;
    private String username;
    private String displayName;
    private int quizId;
    private String quizTitle;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
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
