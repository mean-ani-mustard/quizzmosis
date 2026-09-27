package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Achievement;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizVisibility;
import com.quizzmosis.service.AchievementService;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Step 1 of creating a quiz: title, description and settings. Questions are added on the next page. */
@WebServlet("/quiz/create")
public class CreateQuizServlet extends BaseServlet {

    private final QuizDAO quizDAO = new QuizDAO();
    private final AchievementService achievementService = new AchievementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(currentUser(request).getUserId());
        String error = QuizForm.read(request, quiz);
        if (error != null) {
            request.setAttribute("error", error);
            request.setAttribute("quiz", quiz);
            show(request, response);
            return;
        }

        quizDAO.create(quiz);
        List<Achievement> earned = achievementService.onQuizCreated(quiz.getOwnerId());
        Flash.success(request, "Quiz created! Now add some questions." + achievementText(earned));
        redirect(request, response, "/quiz/questions?id=" + quiz.getQuizId());
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("visibilities", QuizVisibility.values());
        render(request, response, "quiz/create");
    }
}
