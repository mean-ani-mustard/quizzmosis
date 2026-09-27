package com.quizzmosis.web;

import com.quizzmosis.dao.AchievementDAO;
import com.quizzmosis.model.Achievement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** All achievements, with the ones the user has earned highlighted. */
@WebServlet("/achievements")
public class AchievementsServlet extends BaseServlet {

    private final AchievementDAO achievementDAO = new AchievementDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Achievement> achievements = achievementDAO.findAllForUser(currentUser(request).getUserId());
        request.setAttribute("achievements", achievements);
        request.setAttribute("earnedCount", achievements.stream().filter(Achievement::isEarned).count());
        render(request, response, "achievements");
    }
}
