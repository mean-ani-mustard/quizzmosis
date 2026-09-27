package com.quizzmosis.web.filter;

import com.quizzmosis.dao.MessageDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.User;
import com.quizzmosis.web.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Runs before every request (mapped in web.xml) and makes sure only logged-in users reach the
 * application. Login and registration are the only pages open to everyone.
 */
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PAGES = Set.of("/login", "/register");

    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = pathOf(request);

        // CSS, JavaScript and images need no checks.
        if (path.startsWith("/static/") || path.equals("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SessionKeys.USER);
        if (user != null) {
            // Trust the database, not the copy in the session: if an admin deleted this account or
            // took away its admin rights, that takes effect on the very next click.
            user = userDAO.findById(user.getUserId());
            if (user == null) {
                session.invalidate();
            } else {
                session.setAttribute(SessionKeys.USER, user);
            }
        }

        boolean publicPage = PUBLIC_PAGES.contains(path);
        if (user == null && !publicPage) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (user != null && publicPage) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        if (user != null && "GET".equals(request.getMethod())) {
            // The number shown next to "Messages" in the navigation bar.
            request.setAttribute("inboxBadge", messageDAO.countInboxBadge(user.getUserId()));
        }
        // Don't let the browser cache pages, so pressing "Back" after logging out doesn't show them.
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    /**
     * The page being requested, e.g. "/quiz/start". It is taken from the servlet path, which the
     * server has already cleaned up. getRequestURI() would be the raw text the client sent, so a
     * hand-made request for "/static/../home" would look like a static file and skip the checks.
     */
    static String pathOf(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }
}
