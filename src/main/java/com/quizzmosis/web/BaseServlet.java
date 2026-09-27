package com.quizzmosis.web;

import com.quizzmosis.model.Achievement;
import com.quizzmosis.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Small helpers shared by all servlets of the application. */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** The logged-in user. AuthFilter guarantees there is one on every page except login/register. */
    protected static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(SessionKeys.USER);
    }

    /**
     * Shows {@code /WEB-INF/views/<view>.jsp}. The JSPs live under WEB-INF, so the browser can't
     * open them directly and skip the servlet (and its checks).
     */
    protected static void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    /** Redirects to a page of this application, e.g. {@code "/home"}. */
    protected static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /** A request parameter without surrounding spaces, or "" if it is missing. */
    protected static String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    /** A request parameter as a number, or {@code null} if it is missing or not a number. */
    protected static Integer intParam(HttpServletRequest request, String name) {
        try {
            return Integer.valueOf(param(request, name));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Shows the 404 error page. */
    protected static void notFound(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
    }

    /** Shows the 403 error page. */
    protected static void forbidden(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }

    /** " New achievement unlocked: X!" for a flash message, or "" when nothing was earned. */
    protected static String achievementText(List<Achievement> earned) {
        if (earned.isEmpty()) {
            return "";
        }
        String names = earned.stream().map(Achievement::getName).collect(Collectors.joining(", "));
        return (earned.size() == 1 ? " New achievement unlocked: " : " New achievements unlocked: ") + names + "!";
    }
}
