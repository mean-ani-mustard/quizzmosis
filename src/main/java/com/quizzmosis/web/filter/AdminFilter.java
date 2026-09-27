package com.quizzmosis.web.filter;

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

/**
 * Guards everything under {@code /admin}. It runs after AuthFilter, which has just reloaded the
 * user from the database, so a revoked admin is locked out immediately.
 */
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SessionKeys.USER);
        if (user == null || !user.isAdmin()) {
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Only administrators can open this page.");
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }
}
