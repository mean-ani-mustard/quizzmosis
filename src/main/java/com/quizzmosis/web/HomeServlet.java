package com.quizzmosis.web;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.dao.AnnouncementDAO;
import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** The dashboard users see after logging in. */
@WebServlet("/home")
public class HomeServlet extends BaseServlet {

    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();

        request.setAttribute("announcements", announcementDAO.findActive());
        request.setAttribute("achievements", achievementDAO.findEarnedBy(userId));
        request.setAttribute("popularQuizzes", quizDAO.findPopular(userId, 3));
        request.setAttribute("recentQuizzes", quizDAO.findRecent(userId, 3));
        request.setAttribute("myQuizzes", quizDAO.findByOwner(userId, userId));
        request.setAttribute("friends", friendsDAO.findFriends(userId));
        request.setAttribute("friendsActivity", attemptDAO.findRecentByFriendsOf(userId, 8));
        request.setAttribute("myRecentAttempts", attemptDAO.findRecentByUser(userId, userId, 3));
        render(request, response, "home");
    }
}
