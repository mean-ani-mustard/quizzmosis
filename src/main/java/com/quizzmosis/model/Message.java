package com.quizzmosis.model;

import com.quizzmosis.util.Format;

import java.sql.Timestamp;

/**
 * A note (or a reply to one), or an automatic message telling a challenger what happened to their
 * challenge. Also carries the names, quiz title and score the inbox shows, so it needs one query.
 */
public class Message {

    private int messageId;
    private int senderId;
    private String senderDisplayName;
    private int receiverId;
    private String receiverDisplayName;
    private MessageType type;
    private String content;
    /** For replies: the note that started the conversation. {@code null} for that first note. */
    private Integer threadId;
    private Integer challengeId;
    private Integer quizId;
    private String quizTitle;
    /** For CHALLENGE_FINISHED: the friend's result and the comment they left on the quiz. */
    private Integer score;
    private Integer totalQuestions;
    private String resultComment;
    /** The receiver's own best percentage on the quiz, to compare with. */
    private Integer receiverBestPercent;
    private boolean read;
    private Timestamp sentAt;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverDisplayName() {
        return receiverDisplayName;
    }

    public void setReceiverDisplayName(String receiverDisplayName) {
        this.receiverDisplayName = receiverDisplayName;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getThreadId() {
        return threadId;
    }

    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }

    public Integer getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Integer challengeId) {
        this.challengeId = challengeId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
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

    public Integer getPercentage() {
        return score == null || totalQuestions == null ? null : Format.percent(score, totalQuestions);
    }

    public String getResultComment() {
        return resultComment;
    }

    public void setResultComment(String resultComment) {
        this.resultComment = resultComment;
    }

    public Integer getReceiverBestPercent() {
        return receiverBestPercent;
    }

    public void setReceiverBestPercent(Integer receiverBestPercent) {
        this.receiverBestPercent = receiverBestPercent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }
}
