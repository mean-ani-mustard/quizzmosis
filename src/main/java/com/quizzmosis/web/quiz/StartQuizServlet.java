package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSession;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.service.QuizService;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import com.quizzmosis.web.SessionKeys;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Starts a quiz: loads its questions into a {@link QuizSession} kept in the HTTP session and sends
 * the user to the question page. Starting a quiz replaces any quiz that was still in progress.
 */
@WebServlet("/quiz/start")
public class StartQuizServlet extends BaseServlet {

    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizService quizService = new QuizService();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer quizId = intParam(request, "id");
        Quiz quiz = quizId == null ? null : quizDAO.findById(quizId);
        if (quiz == null || !quizAccess.canOpen(quiz, currentUser(request))) {
            notFound(response, "That quiz doesn't exist (any more).");
            return;
        }
        if (!quiz.isPlayable()) {
            Flash.warning(request, "This quiz doesn't have any questions yet.");
            redirect(request, response, "/quiz?id=" + quizId);
            return;
        }
        QuizSession quizSession = quizService.start(quiz, System.currentTimeMillis());
        request.getSession().setAttribute(SessionKeys.QUIZ_SESSION, quizSession);
        redirect(request, response, "/quiz/take");
    }
}
