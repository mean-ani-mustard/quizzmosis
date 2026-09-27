package com.quizzmosis.model;

import com.quizzmosis.util.Format;

import java.sql.Timestamp;

/**
 * One friend challenging another to a quiz, with the names and numbers the pages show next to it.
 * Challenges are never deleted, so they double as the challenge history.
 */
public class Challenge {

    private int challengeId;
    private int challengerId;
    private String challengerName;
    private int challengedId;
    private String challengedName;
    private int quizId;
    private String quizTitle;
    private String message;
    private ChallengeStatus status;
    private String responseComment;
    /** The attempt that completed the challenge; {@code null} until then. */
    private Integer attemptId;
    private Integer score;
    private Integer totalQuestions;
    /** The challenger's best percentage on the quiz, or {@code null} if they never played it. */
    private Integer challengerBestPercent;
    private Timestamp createdAt;
    private Timestamp respondedAt;
    private Timestamp completedAt;

    public int getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(int challengeId) {
        this.challengeId = challengeId;
    }

    public int getChallengerId() {
        return challengerId;
    }

    public void setChallengerId(int challengerId) {
        this.challengerId = challengerId;
    }

    public String getChallengerName() {
        return challengerName;
    }

    public void setChallengerName(String challengerName) {
        this.challengerName = challengerName;
    }

    public int getChallengedId() {
        return challengedId;
    }

    public void setChallengedId(int challengedId) {
        this.challengedId = challengedId;
    }

    public String getChallengedName() {
        return challengedName;
    }

    public void setChallengedName(String challengedName) {
        this.challengedName = challengedName;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public String getResponseComment() {
        return responseComment;
    }

    public void setResponseComment(String responseComment) {
        this.responseComment = responseComment;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    /** The challenged friend's score in percent, or {@code null} while there is no result. */
    public Integer getPercentage() {
        return score == null || totalQuestions == null ? null : Format.percent(score, totalQuestions);
    }

    public Integer getChallengerBestPercent() {
        return challengerBestPercent;
    }

    public void setChallengerBestPercent(Integer challengerBestPercent) {
        this.challengerBestPercent = challengerBestPercent;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}
