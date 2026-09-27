package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizAttemptDAO;
import com.quizzmosis.dao.QuizCommentDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizAttempt;
import com.quizzmosis.model.QuizComment;
import com.quizzmosis.model.User;
import com.quizzmosis.service.ChallengeService;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.util.Format;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Comments on a quiz. A comment is written on the results page after finishing the quiz (one per
 * attempt) and shows up on the quiz page next to the score. If the attempt completed a challenge,
 * the challenger sees the comment together with the score in their messages.
 */
@WebServlet("/quiz/comment")
public class QuizCommentServlet extends BaseServlet {

    private static final int MAX_COMMENT_LENGTH = 1000;

    private final QuizCommentDAO commentDAO = new QuizCommentDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final ChallengeService challengeService = new ChallengeService();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);
        if ("delete".equals(param(request, "action"))) {
            delete(request, response, user);
        } else {
            add(request, response, user);
        }
    }

    private void add(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        Integer attemptId = intParam(request, "attemptId");
        QuizAttempt attempt = attemptId == null ? null : attemptDAO.findById(attemptId);
        // Players comment on their own attempts, and only while they can still see the quiz.
        if (attempt == null || attempt.getUserId() != user.getUserId()) {
            notFound(response, "That result doesn't exist.");
            return;
        }
        Quiz quiz = quizDAO.findById(attempt.getQuizId());
        if (quiz == null || !quizAccess.isVisibleTo(quiz, user.getUserId())) {
            notFound(response, "That quiz doesn't exist (any more).");
            return;
        }
        String content = Format.truncate(param(request, "content"), MAX_COMMENT_LENGTH);
        if (content.isEmpty()) {
            Flash.error(request, "Please write something first.");
        } else if (commentDAO.add(quiz.getQuizId(), user.getUserId(), attemptId, content) < 0) {
            Flash.info(request, "You already left a comment for this attempt.");
        } else {
            List<String> challengers = challengeService.onResultCommented(attemptId);
            Flash.success(request, "Comment posted on the quiz page."
                    + (challengers.isEmpty() ? "" : " " + Format.names(challengers) + " will see it next to your score."));
        }
        redirect(request, response, "/quiz/result?attempt=" + attemptId + "#comment");
    }

    /** Authors can delete their own comments, and admins any comment. */
    private void delete(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        Integer commentId = intParam(request, "commentId");
        QuizComment comment = commentId == null ? null : commentDAO.findById(commentId);
        if (comment == null) {
            notFound(response, "That comment doesn't exist (any more).");
            return;
        }
        if (comment.getUserId() != user.getUserId() && !user.isAdmin()) {
            forbidden(response, "You can only delete your own comments.");
            return;
        }
        commentDAO.delete(comment.getCommentId());
        Flash.info(request, "Comment deleted.");
        redirect(request, response, "/quiz?id=" + comment.getQuizId() + "#comments");
    }
}
