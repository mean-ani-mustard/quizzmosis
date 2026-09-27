package com.quizzmosis.web.social;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.FriendshipStatus;
import com.quizzmosis.model.User;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A short version of a profile (mantra, numbers, achievements and mutual friends) that app.js
 * loads into a pop-up window, so people can look at someone before adding them as a friend.
 * It only returns a piece of HTML, so a browser that opens the address directly is sent to the
 * full profile instead.
 */
@WebServlet("/profile/preview")
public class ProfilePreviewServlet extends BaseServlet {

    /** app.js sends this header; a normal page visit doesn't. */
    private static final String FETCH_HEADER = "X-Requested-With";

    private final UserDAO userDAO = new UserDAO();
    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = intParam(request, "id");
        if (!"fetch".equals(request.getHeader(FETCH_HEADER))) {
            redirect(request, response, id == null ? "/profile" : "/profile?id=" + id);
            return;
        }
        User user = id == null ? null : userDAO.findById(id);
        if (user == null) {
            notFound(response, "That user doesn't exist.");
            return;
        }
        int viewerId = currentUser(request).getUserId();
        FriendshipStatus status = friendsDAO.getStatus(viewerId, id);

        request.setAttribute("profileUser", user);
        request.setAttribute("status", status);
        request.setAttribute("quizCount", quizDAO.findByOwner(id, viewerId).size());
        request.setAttribute("attemptCount", attemptDAO.countByUser(id));
        request.setAttribute("friendCount", friendsDAO.findFriends(id).size());
        request.setAttribute("achievements", achievementDAO.findEarnedBy(id));
        if (status != FriendshipStatus.SELF) {
            request.setAttribute("mutualFriends", friendsDAO.findMutualFriends(viewerId, id));
        }
        render(request, response, "social/profile-preview");
    }
}
