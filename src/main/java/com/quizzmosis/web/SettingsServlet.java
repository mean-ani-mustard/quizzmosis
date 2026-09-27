package com.quizzmosis.web;

import com.quizzmosis.model.User;
import com.quizzmosis.service.AuthService;
import com.quizzmosis.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Account settings: display name, password, and deleting the account. */
@WebServlet("/settings")
public class SettingsServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "settings");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);
        try {
            switch (param(request, "action")) {
                case "name" -> {
                    String name = authService.changeDisplayName(user, param(request, "displayName"));
                    Flash.success(request, "Saved. People now see you as " + name + ".");
                }
                case "password" -> {
                    // Passwords are taken as typed (not trimmed): spaces can be part of a password.
                    authService.changePassword(user, request.getParameter("currentPassword"),
                            request.getParameter("newPassword"), request.getParameter("confirmPassword"));
                    // A new session id after a password change, like after logging in.
                    request.changeSessionId();
                    Flash.success(request, "Your password was changed.");
                }
                case "delete" -> {
                    authService.deleteAccount(user, request.getParameter("password"));
                    request.getSession().invalidate();
                    // The flash message needs a session, so the login page gets a fresh one.
                    Flash.info(request, "Your account and everything in it were deleted. Goodbye!");
                    redirect(request, response, "/login");
                    return;
                }
                default -> {
                }
            }
        } catch (ValidationException e) {
            Flash.error(request, e.getMessage());
        }
        redirect(request, response, "/settings");
    }
}
