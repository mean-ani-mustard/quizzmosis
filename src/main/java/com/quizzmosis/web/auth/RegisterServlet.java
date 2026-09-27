package com.quizzmosis.web.auth;

import com.quizzmosis.model.User;
import com.quizzmosis.service.AuthService;
import com.quizzmosis.service.ValidationException;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import com.quizzmosis.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("minPasswordLength", AuthService.MIN_PASSWORD_LENGTH);
        render(request, response, "auth/register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user;
        try {
            user = authService.register(param(request, "username"), param(request, "displayName"),
                    request.getParameter("password"), request.getParameter("confirmPassword"));
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("username", param(request, "username"));
            request.setAttribute("displayName", param(request, "displayName"));
            request.setAttribute("minPasswordLength", AuthService.MIN_PASSWORD_LENGTH);
            render(request, response, "auth/register");
            return;
        }

        request.changeSessionId();
        HttpSession session = request.getSession();
        session.removeAttribute(SessionKeys.CSRF_TOKEN);
        session.setAttribute(SessionKeys.USER, user);
        Flash.success(request, "Welcome to Quizzmosis, " + user.getDisplayName() + "!");
        redirect(request, response, "/home");
    }
}
