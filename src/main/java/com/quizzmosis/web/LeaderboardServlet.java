package com.quizzmosis.web;

import com.quizzmosis.dao.LeaderboardDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.Timeframe;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Best results across the site, filterable by quiz ({@code ?quiz=}) and time ({@code ?period=}). */
@WebServlet("/leaderboard")
public class LeaderboardServlet extends BaseServlet {

    private static final int ENTRIES = 50;

    private final LeaderboardDAO leaderboardDAO = new LeaderboardDAO();
    private final QuizDAO quizDAO = new QuizDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();
        Timeframe period = Timeframe.fromParam(param(request, "period"));

        List<Quiz> quizzes = quizDAO.findVisibleTo(userId);
        // A quiz this user may not see is ignored, so they get the leaderboard of all quizzes instead.
        Integer requested = intParam(request, "quiz");
        Integer quizId = requested != null && quizzes.stream().anyMatch(quiz -> quiz.getQuizId() == requested)
                ? requested : null;

        List<AttemptSummary> entries = leaderboardDAO.findTop(quizId, period, userId, ENTRIES);
        boolean userIsListed = entries.stream().anyMatch(entry -> entry.getUserId() == userId);

        request.setAttribute("quizzes", quizzes);
        request.setAttribute("selectedQuiz", quizId);
        request.setAttribute("period", period);
        request.setAttribute("periods", Timeframe.values());
        request.setAttribute("entries", entries);
        // Only show the "your best" box when the user isn't already in the table.
        request.setAttribute("personalBest", userIsListed ? null : leaderboardDAO.findPersonalBest(userId, quizId, period));
        render(request, response, "leaderboard");
    }
}
