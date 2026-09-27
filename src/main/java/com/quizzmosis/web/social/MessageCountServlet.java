package com.quizzmosis.web.social;

import com.quizzmosis.dao.MessageDAO;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The number next to "Messages" in the navigation bar, as JSON ({@code {"count": 3}}). app.js
 * asks for it every 30 seconds, so the number stays current without reloading the page.
 */
@WebServlet("/messages/count")
public class MessageCountServlet extends BaseServlet {

    private final MessageDAO messageDAO = new MessageDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int count = messageDAO.countInboxBadge(currentUser(request).getUserId());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"count\": " + count + "}");
    }
}
