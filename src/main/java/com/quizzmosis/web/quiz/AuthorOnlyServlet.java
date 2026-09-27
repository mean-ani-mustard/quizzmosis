package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Base class for the pages where the author of a quiz edits it (settings and questions). */
abstract class AuthorOnlyServlet extends BaseServlet {

    protected final QuizDAO quizDAO = new QuizDAO();

    /** Loads the quiz from ?id= and checks that the current user created it; otherwise sends an error page. */
    protected Quiz findOwnQuiz(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer quizId = intParam(request, "id");
        Quiz quiz = quizId == null ? null : quizDAO.findById(quizId);
        if (quiz == null) {
            notFound(response, "That quiz doesn't exist (any more).");
            return null;
        }
        if (quiz.getOwnerId() != currentUser(request).getUserId()) {
            forbidden(response, "You can only edit quizzes that you created.");
            return null;
        }
        return quiz;
    }
}
