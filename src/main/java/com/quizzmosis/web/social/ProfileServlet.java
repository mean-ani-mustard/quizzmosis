package com.quizzmosis.web.social;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.dao.ChallengeDAO;
import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.FriendshipStatus;
import com.quizzmosis.model.User;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A user's profile ({@code /profile?id=...}; without an id it shows your own). On your own
 * profile you can also set your mantra and hide your challenge history from your friends.
 */
@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {

    private static final int MAX_MANTRA_LENGTH = 150;
    private static final int HISTORY_LIMIT = 20;

    private final UserDAO userDAO = new UserDAO();
    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();
    private final ChallengeDAO challengeDAO = new ChallengeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User me = currentUser(request);
        int viewerId = me.getUserId();
        Integer profileId = intParam(request, "id");
        User profileUser = profileId == null ? me : userDAO.findById(profileId);
        if (profileUser == null) {
            notFound(response, "That user doesn't exist.");
            return;
        }
        int id = profileUser.getUserId();
        FriendshipStatus status = friendsDAO.getStatus(viewerId, id);
        boolean self = status == FriendshipStatus.SELF;
        boolean friends = status == FriendshipStatus.FRIENDS;

        request.setAttribute("profileUser", profileUser);
        request.setAttribute("status", status);
        request.setAttribute("quizzes", quizDAO.findByOwner(id, viewerId));
        request.setAttribute("achievements", achievementDAO.findEarnedBy(id));
        request.setAttribute("attemptCount", attemptDAO.countByUser(id));
        request.setAttribute("friendCount", friendsDAO.findFriends(id).size());
        if (!self) {
            request.setAttribute("mutualFriends", friendsDAO.findMutualFriends(viewerId, id));
        }
        // Recent quiz results are only visible to the user themselves and their friends.
        if (self || friends) {
            request.setAttribute("recentAttempts", attemptDAO.findRecentByUser(id, viewerId, 5));
        }
        // So is the challenge history, unless the user hid it. Hiding never deletes anything:
        // the user still sees their whole history here and in their messages.
        if (self || (friends && profileUser.isShowChallengeHistory())) {
            request.setAttribute("challengeHistory", challengeDAO.findHistory(id, viewerId, HISTORY_LIMIT));
        }
        render(request, response, "social/profile");
    }

    /** Changes to your own profile. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User me = currentUser(request);
        switch (param(request, "action")) {
            case "mantra" -> {
                String mantra = param(request, "mantra");
                if (mantra.length() > MAX_MANTRA_LENGTH) {
                    Flash.error(request, "Your mantra can be at most " + MAX_MANTRA_LENGTH + " characters long.");
                } else {
                    // AuthFilter reloads the user on every request, so the next page shows the change.
                    userDAO.updateMantra(me.getUserId(), mantra.isEmpty() ? null : mantra);
                    Flash.success(request, mantra.isEmpty() ? "Mantra removed." : "Mantra saved!");
                }
                redirect(request, response, "/profile");
            }
            case "history" -> {
                boolean show = "show".equals(param(request, "history"));
                userDAO.setShowChallengeHistory(me.getUserId(), show);
                Flash.info(request, show
                        ? "Your friends can see your challenge history again."
                        : "Your challenge history is hidden from your friends. Nothing was deleted: you still see all of it.");
                redirect(request, response, "/profile#challenge-history");
            }
            default -> redirect(request, response, "/profile");
        }
    }
}
