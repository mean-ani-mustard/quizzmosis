package com.quizzmosis.web.auth;

import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Logging out changes state, so it is a POST (with a CSRF token) rather than a plain link. */
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect(request, response, "/login?loggedOut");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/home");
    }
}
