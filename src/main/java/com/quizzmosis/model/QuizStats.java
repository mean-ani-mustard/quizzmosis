package com.quizzmosis.model;

import com.quizzmosis.util.Format;

/** Summary numbers shown at the top of a quiz page. */
public class QuizStats {

    private int questionCount;
    private int attemptCount;
    private double averagePercent;
    private double averageDurationSeconds;
    private int highestScore;

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public double getAveragePercent() {
        return averagePercent;
    }

    public void setAveragePercent(double averagePercent) {
        this.averagePercent = averagePercent;
    }

    public double getAverageDurationSeconds() {
        return averageDurationSeconds;
    }

    public void setAverageDurationSeconds(double averageDurationSeconds) {
        this.averageDurationSeconds = averageDurationSeconds;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public String getFormattedAverageDuration() {
        return Format.duration((int) Math.round(averageDurationSeconds));
    }
}
