package com.quizzmosis.web.auth;

import com.quizzmosis.model.User;
import com.quizzmosis.service.AuthService;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = param(request, "username");
        User user = authService.login(username, request.getParameter("password"));
        if (user == null) {
            request.setAttribute("error", "Invalid username or password.");
            request.setAttribute("username", username);
            render(request, response, "auth/login");
            return;
        }

        // Give the logged-in user a new session id so an id an attacker planted before login is
        // worthless afterwards (session fixation), and start with a fresh CSRF token.
        request.changeSessionId();
        HttpSession session = request.getSession();
        session.removeAttribute(SessionKeys.CSRF_TOKEN);
        session.setAttribute(SessionKeys.USER, user);
        redirect(request, response, "/home");
    }
}
