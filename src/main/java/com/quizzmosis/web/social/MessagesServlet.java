package com.quizzmosis.web.social;

import com.quizzmosis.dao.ChallengeDAO;
import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.MessageDAO;
import com.quizzmosis.model.Challenge;
import com.quizzmosis.model.Message;
import com.quizzmosis.model.NoteThread;
import com.quizzmosis.model.User;
import com.quizzmosis.service.ChallengeService;
import com.quizzmosis.service.ValidationException;
import com.quizzmosis.util.Format;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The inbox, with four tabs: friend requests, challenges (to answer, and replies to the ones you
 * sent), notes (conversations you can reply to) and the challenge history. It also handles the
 * forms on those tabs.
 */
@WebServlet("/messages")
public class MessagesServlet extends BaseServlet {

    private static final Set<String> TABS = Set.of("requests", "challenges", "notes", "history");
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int HISTORY_LIMIT = 100;

    private final MessageDAO messageDAO = new MessageDAO();
    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final ChallengeDAO challengeDAO = new ChallengeDAO();
    private final ChallengeService challengeService = new ChallengeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();
        List<User> requests = friendsDAO.findIncomingRequests(userId);
        List<User> friends = friendsDAO.findFriends(userId);
        List<Challenge> pendingChallenges = challengeDAO.findPendingFor(userId);
        List<Message> challengeUpdates = messageDAO.findChallengeUpdates(userId);
        List<NoteThread> threads = messageDAO.findThreads(userId);

        // You can only reply while you are still friends.
        Set<Integer> friendIds = friends.stream().map(User::getUserId).collect(Collectors.toSet());
        threads.forEach(thread -> thread.setReplyAllowed(friendIds.contains(thread.getOtherUserId())));

        long unreadUpdates = challengeUpdates.stream().filter(message -> !message.isRead()).count();
        long unreadThreads = threads.stream().filter(NoteThread::isUnread).count();
        String tab = param(request, "tab");
        if (!TABS.contains(tab)) {
            // Open the first tab that has something new.
            tab = !requests.isEmpty() ? "requests"
                    : !pendingChallenges.isEmpty() || unreadUpdates > 0 ? "challenges"
                    : unreadThreads > 0 ? "notes" : "challenges";
        }

        request.setAttribute("requests", requests);
        request.setAttribute("pendingChallenges", pendingChallenges);
        request.setAttribute("challengeUpdates", challengeUpdates);
        request.setAttribute("threads", threads);
        request.setAttribute("history", challengeDAO.findHistory(userId, userId, HISTORY_LIMIT));
        request.setAttribute("friends", friends);
        request.setAttribute("noteTo", intParam(request, "to"));
        request.setAttribute("unreadUpdates", unreadUpdates);
        request.setAttribute("unreadThreads", unreadThreads);
        request.setAttribute("tab", tab);

        // Everything is on screen now, so nothing is unread any more. The objects above keep their
        // old flag, which lets the page highlight what was new on this visit.
        messageDAO.markAllRead(userId);
        request.setAttribute("inboxBadge", requests.size() + pendingChallenges.size());
        render(request, response, "social/messages");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = currentUser(request).getUserId();
        switch (param(request, "action")) {
            case "send_note" -> sendNote(request, response, userId);
            case "reply" -> reply(request, response, userId);
            case "accept" -> answerChallenge(request, response, userId, true);
            case "decline" -> answerChallenge(request, response, userId, false);
            default -> redirect(request, response, "/messages");
        }
    }

    /** Starts a new conversation with a friend. */
    private void sendNote(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        Integer receiverId = intParam(request, "receiverId");
        String content = param(request, "content");
        if (receiverId == null || !friendsDAO.areFriends(userId, receiverId)) {
            Flash.error(request, "You can only send notes to your friends.");
        } else if (content.isEmpty()) {
            Flash.error(request, "Please write something first.");
        } else {
            messageDAO.sendNote(userId, receiverId, Format.truncate(content, MAX_NOTE_LENGTH));
            Flash.success(request, "Note sent!");
        }
        redirect(request, response, "/messages?tab=notes");
    }

    /** Adds a reply underneath a conversation the user takes part in. */
    private void reply(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        Integer threadId = intParam(request, "threadId");
        Message first = threadId == null ? null : messageDAO.findNote(threadId);
        String content = param(request, "content");
        // The id must be the first note of a conversation between this user and someone else.
        if (first == null || first.getThreadId() != null
                || (first.getSenderId() != userId && first.getReceiverId() != userId)) {
            Flash.error(request, "That conversation doesn't exist.");
            redirect(request, response, "/messages?tab=notes");
            return;
        }
        int otherId = first.getSenderId() == userId ? first.getReceiverId() : first.getSenderId();
        if (!friendsDAO.areFriends(userId, otherId)) {
            Flash.error(request, "You can only reply to your friends.");
        } else if (content.isEmpty()) {
            Flash.error(request, "Please write something first.");
        } else {
            messageDAO.reply(threadId, userId, otherId, Format.truncate(content, MAX_NOTE_LENGTH));
            Flash.success(request, "Reply sent!");
        }
        redirect(request, response, "/messages?tab=notes#thread-" + threadId);
    }

    /** Accepts (and starts the quiz) or declines a challenge; the challenger gets a message either way. */
    private void answerChallenge(HttpServletRequest request, HttpServletResponse response, int userId, boolean accept)
            throws IOException {
        Integer challengeId = intParam(request, "challengeId");
        String comment = param(request, "comment");
        try {
            if (challengeId == null) {
                throw new ValidationException("That challenge doesn't exist.");
            }
            if (accept) {
                Challenge challenge = challengeService.accept(challengeId, userId, comment);
                Flash.success(request, "Challenge accepted! " + challenge.getChallengerName()
                        + " will get your score when you finish.");
                redirect(request, response, "/quiz/start?id=" + challenge.getQuizId());
            } else {
                Challenge challenge = challengeService.decline(challengeId, userId, comment);
                Flash.info(request, "Challenge declined. " + challenge.getChallengerName() + " has been told.");
                redirect(request, response, "/messages?tab=challenges");
            }
        } catch (ValidationException e) {
            Flash.error(request, e.getMessage());
            redirect(request, response, "/messages?tab=challenges");
        }
    }
}
