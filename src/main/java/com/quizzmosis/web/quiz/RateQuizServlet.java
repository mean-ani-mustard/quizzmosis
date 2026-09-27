package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.QuizRatingDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.User;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Saves a 1-5 star rating of a quiz. Only players who finished the quiz can rate it, and authors
 * can't rate their own quizzes. Rating again replaces the earlier rating.
 */
@WebServlet("/quiz/rate")
public class RateQuizServlet extends BaseServlet {

    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizRatingDAO ratingDAO = new QuizRatingDAO();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);
        Integer quizId = intParam(request, "quizId");
        Quiz quiz = quizId == null ? null : quizDAO.findById(quizId);
        if (quiz == null || !quizAccess.isVisibleTo(quiz, user.getUserId())) {
            notFound(response, "That quiz doesn't exist (any more).");
            return;
        }
        Integer rating = intParam(request, "rating");
        if (!quizAccess.canRate(quiz, user.getUserId())) {
            Flash.error(request, quiz.getOwnerId() == user.getUserId()
                    ? "You can't rate your own quiz." : "Finish the quiz first, then you can rate it.");
        } else if (rating == null || rating < 1 || rating > 5) {
            Flash.error(request, "Please choose from 1 to 5 stars.");
        } else {
            ratingDAO.rate(quiz.getQuizId(), user.getUserId(), rating);
            Flash.success(request, "Thanks for rating! You gave it " + rating + (rating == 1 ? " star." : " stars."));
        }
        // Back to the page the form was on: a result page, or the quiz page.
        Integer attemptId = intParam(request, "attempt");
        redirect(request, response, attemptId != null
                ? "/quiz/result?attempt=" + attemptId + "#rating"
                : "/quiz?id=" + quiz.getQuizId() + "#rating");
    }
}
