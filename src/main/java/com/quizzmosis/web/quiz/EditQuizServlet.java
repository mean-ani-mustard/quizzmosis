package com.quizzmosis.web.quiz;

import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizVisibility;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Changing a quiz's title, description and settings after it was created (only its author can). */
@WebServlet("/quiz/edit")
public class EditQuizServlet extends AuthorOnlyServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = findOwnQuiz(request, response);
        if (quiz != null) {
            show(request, response, quiz);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = findOwnQuiz(request, response);
        if (quiz == null) {
            return;
        }
        String error = QuizForm.read(request, quiz);
        if (error != null) {
            request.setAttribute("error", error);
            show(request, response, quiz);
            return;
        }
        quizDAO.update(quiz);
        Flash.success(request, "Quiz settings saved.");
        redirect(request, response, "/quiz/questions?id=" + quiz.getQuizId());
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Quiz quiz)
            throws ServletException, IOException {
        request.setAttribute("quiz", quiz);
        request.setAttribute("editing", true);
        request.setAttribute("visibilities", QuizVisibility.values());
        render(request, response, "quiz/create");
    }
}
