package com.quizzmosis.model;

import java.sql.Timestamp;

/** A quiz plus a few numbers that the list pages show next to it. */
public class Quiz {

    private int quizId;
    private int ownerId;
    private String ownerUsername;
    private String title;
    private String description;
    private boolean randomOrder;
    private boolean multiplePages;
    private boolean immediateCorrection;
    private QuizVisibility visibility = QuizVisibility.PUBLIC;
    /** How long players have to finish the quiz, or {@code null} for no time limit. */
    private Integer timeLimitSeconds;
    private Timestamp creationDate;
    private int questionCount;
    private int attemptCount;
    /** The average star rating, or {@code null} while nobody has rated the quiz. */
    private Double averageRating;
    private int ratingCount;

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRandomOrder() {
        return randomOrder;
    }

    public void setRandomOrder(boolean randomOrder) {
        this.randomOrder = randomOrder;
    }

    public boolean isMultiplePages() {
        return multiplePages;
    }

    public void setMultiplePages(boolean multiplePages) {
        this.multiplePages = multiplePages;
    }

    public boolean isImmediateCorrection() {
        return immediateCorrection;
    }

    public void setImmediateCorrection(boolean immediateCorrection) {
        this.immediateCorrection = immediateCorrection;
    }

    public QuizVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(QuizVisibility visibility) {
        this.visibility = visibility;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

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

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    /** The time limit in whole minutes (how the form asks for it), or {@code null}. */
    public Integer getTimeLimitMinutes() {
        return timeLimitSeconds == null ? null : timeLimitSeconds / 60;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    /** A quiz without questions can't be played yet. */
    public boolean isPlayable() {
        return questionCount > 0;
    }
}
