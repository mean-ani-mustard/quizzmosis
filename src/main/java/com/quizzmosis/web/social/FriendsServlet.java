package com.quizzmosis.web.social;

import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.FriendshipStatus;
import com.quizzmosis.model.User;
import com.quizzmosis.model.UserWithStatus;
import com.quizzmosis.util.Page;
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
 * Friends page: your friends and pending requests, plus ways to find new friends (search, friends
 * of friends, and a list of everybody else). Every friendship button on the site (profile,
 * profile preview, messages, search results) posts here.
 */
@WebServlet("/friends")
public class FriendsServlet extends BaseServlet {

    private static final int SEARCH_LIMIT = 20;
    private static final int SUGGESTION_LIMIT = 5;
    private static final int PAGE_SIZE = 20;

    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();
        request.setAttribute("friends", friendsDAO.findFriends(userId));
        request.setAttribute("incoming", friendsDAO.findIncomingRequests(userId));
        request.setAttribute("outgoing", friendsDAO.findOutgoingRequests(userId));

        List<UserWithStatus> suggestions = friendsDAO.findSuggestions(userId, SUGGESTION_LIMIT);
        Set<Integer> suggestedIds = suggestions.stream().map(person -> person.getUser().getUserId())
                .collect(Collectors.toSet());
        request.setAttribute("suggestions", suggestions);
        // Everybody else who isn't a friend yet (the suggestions are already listed above), a page at a time.
        request.setAttribute("people", Page.load(intParam(request, "page"), PAGE_SIZE,
                friendsDAO.countPeople(userId, suggestedIds),
                (limit, offset) -> friendsDAO.findPeople(userId, suggestedIds, limit, offset)));

        String query = param(request, "q");
        if (!query.isEmpty()) {
            request.setAttribute("query", query);
            request.setAttribute("results", friendsDAO.searchPeople(userId, query, SEARCH_LIMIT));
        }
        render(request, response, "social/friends");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = currentUser(request).getUserId();
        Integer otherId = intParam(request, "userId");
        User other = otherId == null ? null : userDAO.findById(otherId);
        if (other == null || other.getUserId() == userId) {
            Flash.error(request, "That user doesn't exist.");
            redirect(request, response, "/friends");
            return;
        }

        String name = other.getDisplayName();
        switch (param(request, "action")) {
            case "send" -> {
                FriendshipStatus status = friendsDAO.sendRequest(userId, other.getUserId());
                Flash.success(request, status == FriendshipStatus.FRIENDS
                        ? "You and " + name + " are now friends!"
                        : "Friend request sent to " + name + ".");
            }
            case "accept" -> {
                if (friendsDAO.acceptRequest(userId, other.getUserId())) {
                    Flash.success(request, "You and " + name + " are now friends!");
                }
            }
            case "reject" -> {
                if (friendsDAO.rejectRequest(userId, other.getUserId())) {
                    Flash.info(request, "Friend request from " + name + " declined.");
                }
            }
            case "cancel" -> {
                if (friendsDAO.cancelRequest(userId, other.getUserId())) {
                    Flash.info(request, "Friend request to " + name + " cancelled.");
                }
            }
            case "remove" -> {
                if (friendsDAO.removeFriendship(userId, other.getUserId())) {
                    Flash.info(request, "You are no longer friends with " + name + ".");
                }
            }
            default -> Flash.error(request, "Unknown action.");
        }

        // Go back to the page the button was on. Only these fixed targets are possible, so the
        // parameter can't be abused to redirect users to another website.
        switch (param(request, "returnTo")) {
            case "profile" -> redirect(request, response, "/profile?id=" + other.getUserId());
            case "messages" -> redirect(request, response, "/messages?tab=requests");
            default -> redirect(request, response, "/friends");
        }
    }
}
