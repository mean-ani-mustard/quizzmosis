package com.quizzmosis.web.filter;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protection against Cross-Site Request Forgery (CSRF).
 *
 * <p>Without it, any other website could contain a hidden form that posts to, say,
 * {@code /admin} with {@code action=delete_user}; if a logged-in admin visits that site, their
 * browser sends the session cookie along and the request would succeed.</p>
 *
 * <p>The fix: every session gets a random secret token. Every POST form in the JSPs includes it in
 * a hidden {@code _csrf} field, and this filter rejects POST requests that don't send the right
 * token. Other websites can't read the token, so they can't forge a valid form.</p>
 */
public class CsrfFilter implements Filter {

    public static final String PARAMETER = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (AuthFilter.pathOf(request).startsWith("/static/")) {
            chain.doFilter(request, response);
            return;
        }

        // A session is created even before login, because the login form needs a token too.
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(SessionKeys.CSRF_TOKEN);
        if (token == null) {
            token = newToken();
            session.setAttribute(SessionKeys.CSRF_TOKEN, token);
        }
        // JSPs put it into their forms with: <input type="hidden" name="_csrf" value="${csrfToken}">
        request.setAttribute("csrfToken", token);

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String sent = request.getParameter(PARAMETER);
            if (sent == null || !MessageDigest.isEqual(
                    sent.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "This form has expired. Please go back, reload the page and try again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
