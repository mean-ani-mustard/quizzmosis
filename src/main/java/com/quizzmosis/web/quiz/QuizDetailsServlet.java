package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizCommentDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.QuizRatingDAO;
import com.quizzmosis.dao.QuizStatsDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.User;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * The summary page of one quiz: description, statistics, your attempts, the best players and the
 * comments players left after taking it.
 */
@WebServlet("/quiz")
public class QuizDetailsServlet extends BaseServlet {

    private static final Set<String> SORT_OPTIONS = Set.of("date", "score", "time");

    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizStatsDAO statsDAO = new QuizStatsDAO();
    private final QuizCommentDAO commentDAO = new QuizCommentDAO();
    private final QuizRatingDAO ratingDAO = new QuizRatingDAO();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer quizId = intParam(request, "id");
        Quiz quiz = quizId == null ? null : quizDAO.findById(quizId);
        User user = currentUser(request);
        if (quiz == null || !quizAccess.canOpen(quiz, user)) {
            // A hidden quiz gets the same answer as a missing one, so nobody can tell it exists.
            notFound(response, "That quiz doesn't exist (any more).");
            return;
        }
        String sort = SORT_OPTIONS.contains(param(request, "sort")) ? param(request, "sort") : "date";

        request.setAttribute("quiz", quiz);
        request.setAttribute("isOwner", quiz.getOwnerId() == user.getUserId());
        request.setAttribute("stats", statsDAO.getStats(quizId));
        request.setAttribute("sort", sort);
        request.setAttribute("myAttempts", statsDAO.findUserAttempts(user.getUserId(), quizId, sort));
        request.setAttribute("topAllTime", statsDAO.findTopPerformers(quizId, false, 10));
        request.setAttribute("topLastDay", statsDAO.findTopPerformers(quizId, true, 5));
        request.setAttribute("recentAttempts", statsDAO.findRecentAttempts(quizId, 10));
        request.setAttribute("comments", commentDAO.findByQuiz(quizId));
        request.setAttribute("canRate", quizAccess.canRate(quiz, user.getUserId()));
        request.setAttribute("myRating", ratingDAO.findRating(quizId, user.getUserId()));
        render(request, response, "quiz/details");
    }
}
