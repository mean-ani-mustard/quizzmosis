package com.quizzmosis.web.admin;

import com.quizzmosis.dao.AnnouncementDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.User;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Administrator dashboard. AdminFilter (see web.xml) lets only admins reach this servlet, so the
 * methods below don't need to check that again.
 */
@WebServlet("/admin")
public class AdminServlet extends BaseServlet {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final UserDAO userDAO = new UserDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("userCount", userDAO.count());
        request.setAttribute("quizCount", quizDAO.count());
        request.setAttribute("attemptCount", attemptDAO.count());
        request.setAttribute("users", userDAO.findAll());
        request.setAttribute("quizzes", quizDAO.findAll());
        request.setAttribute("announcements", announcementDAO.findAll());
        render(request, response, "admin/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User admin = currentUser(request);
        Integer id = intParam(request, "id");

        switch (param(request, "action")) {
            case "create_announcement" -> {
                String title = param(request, "title");
                String content = param(request, "content");
                if (title.isEmpty() || content.isEmpty()) {
                    Flash.error(request, "An announcement needs a title and some text.");
                } else if (title.length() > MAX_TITLE_LENGTH || content.length() > MAX_CONTENT_LENGTH) {
                    Flash.error(request, "Announcement titles can be " + MAX_TITLE_LENGTH
                            + " characters long and the text " + MAX_CONTENT_LENGTH + " characters.");
                } else {
                    announcementDAO.create(title, content, admin.getUserId());
                    Flash.success(request, "Announcement published.");
                }
            }
            case "toggle_announcement" -> {
                if (id != null && announcementDAO.toggleActive(id)) {
                    Flash.success(request, "Announcement updated.");
                }
            }
            case "delete_announcement" -> {
                if (id != null && announcementDAO.delete(id)) {
                    Flash.success(request, "Announcement deleted.");
                }
            }
            case "promote" -> {
                if (id != null && userDAO.setAdmin(id, true)) {
                    Flash.success(request, "User promoted to administrator.");
                }
            }
            case "demote" -> {
                if (id != null && id == admin.getUserId()) {
                    Flash.error(request, "You can't remove your own administrator rights.");
                } else if (id != null && userDAO.setAdmin(id, false)) {
                    Flash.success(request, "Administrator rights removed.");
                }
            }
            case "delete_user" -> {
                if (id != null && id == admin.getUserId()) {
                    Flash.error(request, "You can't delete your own account from here.");
                } else if (id != null && userDAO.delete(id)) {
                    Flash.success(request, "User deleted together with their quizzes and activity.");
                }
            }
            case "delete_quiz" -> {
                if (id != null && quizDAO.delete(id)) {
                    Flash.success(request, "Quiz deleted.");
                }
            }
            case "clear_history" -> {
                if (id != null) {
                    int removed = quizDAO.clearHistory(id);
                    Flash.success(request, "Removed " + removed + " attempt(s) from the quiz history.");
                }
            }
            default -> Flash.error(request, "Unknown action.");
        }
        redirect(request, response, "/admin");
    }
}
