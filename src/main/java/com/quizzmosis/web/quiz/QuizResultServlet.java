package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.ChallengeDAO;
import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizCommentDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.QuizRatingDAO;
import com.quizzmosis.model.Challenge;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizAttempt;
import com.quizzmosis.model.QuizComment;
import com.quizzmosis.model.User;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.util.Format;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Results of one finished attempt. It's a normal GET page, so refreshing it is harmless (in the
 * first version, refreshing the results page crashed because the quiz state was already gone).
 * The player can leave a comment on the quiz from here.
 */
@WebServlet("/quiz/result")
public class QuizResultServlet extends BaseServlet {

    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizCommentDAO commentDAO = new QuizCommentDAO();
    private final ChallengeDAO challengeDAO = new ChallengeDAO();
    private final QuizRatingDAO ratingDAO = new QuizRatingDAO();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attemptId = intParam(request, "attempt");
        QuizAttempt attempt = attemptId == null ? null : attemptDAO.findById(attemptId);
        if (attempt == null) {
            notFound(response, "That result doesn't exist.");
            return;
        }
        User user = currentUser(request);
        boolean ownAttempt = attempt.getUserId() == user.getUserId();
        if (!ownAttempt && !user.isAdmin()) {
            forbidden(response, "You can only look at your own results.");
            return;
        }
        Quiz quiz = quizDAO.findById(attempt.getQuizId());
        if (quiz == null) {
            notFound(response, "That quiz doesn't exist any more.");
            return;
        }
        QuizComment comment = commentDAO.findByAttempt(attemptId);

        request.setAttribute("attempt", attempt);
        request.setAttribute("quiz", quiz);
        request.setAttribute("answers", attemptDAO.findAnswers(attemptId));
        request.setAttribute("comment", comment);
        // One comment per attempt, written by the player while they can still see the quiz.
        request.setAttribute("canComment", ownAttempt && comment == null && quizAccess.isVisibleTo(quiz, user.getUserId()));
        // The player can rate the quiz here too (or change their rating).
        request.setAttribute("canRate", ownAttempt && quizAccess.canRate(quiz, user.getUserId()));
        request.setAttribute("myRating", ratingDAO.findRating(quiz.getQuizId(), user.getUserId()));
        // The friends who challenged the player to this quiz and got this result ("" if none).
        request.setAttribute("challengerNames", Format.names(challengeDAO.findCompletedBy(attemptId).stream()
                .map(Challenge::getChallengerName).toList()));
        render(request, response, "quiz/result");
    }
}
