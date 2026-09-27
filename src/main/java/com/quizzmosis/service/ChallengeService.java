package com.quizzmosis.service;

import com.quizzmosis.dao.ChallengeDAO;
import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.MessageDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Challenge;
import com.quizzmosis.model.ChallengeStatus;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.util.Format;

import java.util.List;

/**
 * The life of a challenge: a friend sends it, the challenged user accepts or declines it (with an
 * optional comment), and an accepted challenge is completed when they finish the quiz. After each
 * step the challenger gets a message.
 */
public class ChallengeService {

    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_COMMENT_LENGTH = 500;
    private static final String DEFAULT_MESSAGE = "Can you beat my score?";

    private final ChallengeDAO challenges;
    private final MessageDAO messages;
    private final FriendsDAO friends;
    private final QuizDAO quizzes;
    private final QuizAccess quizAccess;

    public ChallengeService() {
        this(new ChallengeDAO(), new MessageDAO(), new FriendsDAO(), new QuizDAO());
    }

    public ChallengeService(ChallengeDAO challenges, MessageDAO messages, FriendsDAO friends, QuizDAO quizzes) {
        this.challenges = challenges;
        this.messages = messages;
        this.friends = friends;
        this.quizzes = quizzes;
        this.quizAccess = new QuizAccess(friends);
    }

    /**
     * Challenges a friend to a quiz.
     *
     * @return the new challenge's id
     * @throws ValidationException if they aren't friends, can't see the quiz, or the same
     *                             challenge is still waiting for an answer
     */
    public int send(int challengerId, int challengedId, Quiz quiz, String message) throws ValidationException {
        if (!friends.areFriends(challengerId, challengedId)) {
            throw new ValidationException("You can only challenge your friends.");
        }
        if (!quizAccess.isVisibleTo(quiz, challengedId)) {
            throw new ValidationException("That friend can't see this quiz, so you can't challenge them to it.");
        }
        if (challenges.hasPending(challengerId, challengedId, quiz.getQuizId())) {
            throw new ValidationException("You already challenged them to this quiz. Wait for their answer first.");
        }
        String text = message == null || message.isBlank() ? DEFAULT_MESSAGE : message.trim();
        return challenges.create(challengerId, challengedId, quiz.getQuizId(), Format.truncate(text, MAX_MESSAGE_LENGTH));
    }

    /**
     * Accepts a challenge and tells the challenger.
     *
     * @return the challenge, so the caller can start its quiz
     */
    public Challenge accept(int challengeId, int userId, String comment) throws ValidationException {
        Challenge challenge = findOwnPending(challengeId, userId);
        Quiz quiz = quizzes.findById(challenge.getQuizId());
        if (quiz == null || !quizAccess.isVisibleTo(quiz, userId)) {
            throw new ValidationException("This quiz isn't available any more.");
        }
        return answer(challenge, userId, comment, ChallengeStatus.ACCEPTED, MessageType.CHALLENGE_ACCEPTED);
    }

    /** Declines a challenge and tells the challenger. */
    public Challenge decline(int challengeId, int userId, String comment) throws ValidationException {
        Challenge challenge = findOwnPending(challengeId, userId);
        return answer(challenge, userId, comment, ChallengeStatus.DECLINED, MessageType.CHALLENGE_DECLINED);
    }

    /**
     * Call after a finished attempt was saved: completes the challenges the user accepted for this
     * quiz and sends each challenger the result.
     *
     * @return the names of the challengers who got the result
     */
    public List<String> onQuizFinished(int userId, int quizId, int attemptId) {
        List<Challenge> completed = challenges.completeAccepted(userId, quizId, attemptId);
        for (Challenge challenge : completed) {
            messages.sendChallengeUpdate(MessageType.CHALLENGE_FINISHED, userId, challenge.getChallengerId(),
                    challenge.getChallengeId(), null);
        }
        return completed.stream().map(Challenge::getChallengerName).toList();
    }

    /**
     * Call after the player commented on an attempt: if the attempt completed challenges, the
     * challengers see their result message as new again, now with the comment.
     *
     * @return the names of those challengers
     */
    public List<String> onResultCommented(int attemptId) {
        List<Challenge> completed = challenges.findCompletedBy(attemptId);
        if (!completed.isEmpty()) {
            messages.markResultUnread(attemptId);
        }
        return completed.stream().map(Challenge::getChallengerName).toList();
    }

    private Challenge findOwnPending(int challengeId, int userId) throws ValidationException {
        Challenge challenge = challenges.findById(challengeId);
        if (challenge == null || challenge.getChallengedId() != userId) {
            throw new ValidationException("That challenge doesn't exist.");
        }
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new ValidationException("You already answered that challenge.");
        }
        return challenge;
    }

    private Challenge answer(Challenge challenge, int userId, String comment, ChallengeStatus answer,
                             MessageType messageType) throws ValidationException {
        String text = comment == null || comment.isBlank() ? null : Format.truncate(comment.trim(), MAX_COMMENT_LENGTH);
        // The UPDATE only changes a challenge that is still pending, so a double click can't answer twice.
        if (!challenges.respond(challenge.getChallengeId(), userId, answer, text)) {
            throw new ValidationException("You already answered that challenge.");
        }
        messages.sendChallengeUpdate(messageType, userId, challenge.getChallengerId(), challenge.getChallengeId(), text);
        challenge.setStatus(answer);
        challenge.setResponseComment(text);
        return challenge;
    }
}
