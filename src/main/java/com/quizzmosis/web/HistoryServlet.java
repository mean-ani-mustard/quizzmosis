package com.quizzmosis.web;

import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.util.Page;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Every quiz the current user has finished, newest first, 20 per page. */
@WebServlet("/history")
public class HistoryServlet extends BaseServlet {

    private static final int PAGE_SIZE = 20;

    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();
        request.setAttribute("page", Page.load(intParam(request, "page"), PAGE_SIZE, attemptDAO.countHistory(userId),
                (limit, offset) -> attemptDAO.findHistory(userId, limit, offset)));
        render(request, response, "history");
    }
}
