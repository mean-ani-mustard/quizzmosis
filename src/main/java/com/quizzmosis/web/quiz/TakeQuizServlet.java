package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.DataAccessException;
import com.quizzmosis.model.AttemptAnswer;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuizSession;
import com.quizzmosis.service.QuizGrader;
import com.quizzmosis.service.QuizService;
import com.quizzmosis.service.QuizService.FinishedQuiz;
import com.quizzmosis.util.Format;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import com.quizzmosis.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Taking a quiz.
 *
 * <ul>
 *   <li>GET shows the current question (multi-page quizzes) or all questions (single-page quizzes).</li>
 *   <li>POST grades the submitted answer(s). Single-page quizzes and the last page of a multi-page
 *       quiz are then finished: the attempt is saved and the user is sent to the results page.</li>
 *   <li>With immediate correction, each answer is followed by a feedback page; its "See results"
 *       button posts {@code action=finish}.</li>
 *   <li>With a time limit, the page counts down and sends what was answered when time runs out.
 *       The server checks the time too: once it is up the quiz is finished, and answers that
 *       arrive too late don't count.</li>
 * </ul>
 */
@WebServlet("/quiz/take")
public class TakeQuizServlet extends BaseServlet {

    private final QuizService quizService = new QuizService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        QuizSession quizSession = quizInProgress(request, response);
        if (quizSession == null) {
            return;
        }
        request.setAttribute("quizSession", quizSession);
        long now = System.currentTimeMillis();
        Integer secondsLeft = quizSession.getSecondsLeft(now);
        if (secondsLeft != null) {
            request.setAttribute("secondsLeft", secondsLeft);
            request.setAttribute("timeLeft", Format.duration(secondsLeft));
            request.setAttribute("timeUp", quizSession.isTimeUp(now));
        }
        AttemptAnswer feedback = quizSession.takePendingFeedback();
        if (feedback != null) {
            request.setAttribute("feedback", feedback);
            request.setAttribute("feedbackQuestion", quizSession.findQuestion(feedback.getQuestionId()));
        }
        render(request, response, "quiz/take");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        QuizSession quizSession = quizInProgress(request, response);
        if (quizSession == null) {
            return;
        }
        Integer quizId = intParam(request, "quizId");
        if (quizId == null || quizId != quizSession.getQuizId()) {
            // A page of a quiz that is no longer in progress, e.g. because another quiz was started
            // in a second tab. Without this check it would finish the other quiz with no answers.
            Flash.warning(request, "That page belonged to a quiz you're no longer taking, so it was ignored.");
            redirect(request, response, "/quiz/take");
            return;
        }
        long now = System.currentTimeMillis();
        if ("finish".equals(param(request, "action")) || quizSession.isTooLate(now)) {
            // Answers sent after the time limit (and its few seconds of grace) are not recorded.
            finish(request, response, quizSession, quizSession.isTimeUp(now));
            return;
        }

        if (quizSession.isMultiplePages()) {
            Question current = quizSession.getCurrentQuestion();
            Integer questionId = intParam(request, "questionId");
            // Only the question that is currently on screen can be answered. A form sent twice
            // (double click, Back button) points at an older question and is simply ignored.
            if (current != null && questionId != null && questionId == current.getQuestionId()) {
                quizSession.recordAnswer(QuizGrader.grade(current, request.getParameter("answer")));
            }
            if (quizSession.isTimeUp(now)) {
                finish(request, response, quizSession, true);
            } else if (quizSession.isComplete() && !quizSession.isImmediateCorrection()) {
                finish(request, response, quizSession, false);
            } else {
                redirect(request, response, "/quiz/take");
            }
        } else {
            for (Question question : quizSession.getQuestions()) {
                String answer = request.getParameter("answer_" + question.getQuestionId());
                quizSession.recordAnswer(QuizGrader.grade(question, answer));
            }
            finish(request, response, quizSession, quizSession.isTimeUp(now));
        }
    }

    /** Saves the attempt and shows its results. {@code timeUp}: the time limit ended the quiz. */
    private void finish(HttpServletRequest request, HttpServletResponse response, QuizSession quizSession,
                        boolean timeUp) throws IOException {
        if (!quizSession.tryMarkFinished()) {
            // The same quiz was already finished by an earlier (double-clicked) request.
            redirect(request, response, "/history");
            return;
        }
        FinishedQuiz result;
        try {
            result = quizService.finish(currentUser(request).getUserId(), quizSession, System.currentTimeMillis());
        } catch (DataAccessException e) {
            if (!e.isConstraintViolation()) {
                throw e;
            }
            // A foreign key failed: the quiz or one of its questions was deleted while the user played.
            request.getSession().removeAttribute(SessionKeys.QUIZ_SESSION);
            Flash.warning(request, "This quiz was changed while you were playing it, so your result couldn't be saved. "
                    + "Please start it again.");
            redirect(request, response, "/quizzes");
            return;
        }
        request.getSession().removeAttribute(SessionKeys.QUIZ_SESSION);
        String news = result.newAchievements().isEmpty() ? "" : "Well done!" + achievementText(result.newAchievements());
        if (!result.challengersNotified().isEmpty()) {
            // The accepted challenges are now completed and the challengers got this result.
            news += " Your score was sent to " + Format.names(result.challengersNotified()) + ".";
        }
        if (timeUp) {
            Flash.warning(request, ("Time's up! Questions you didn't answer count as wrong. " + news).trim());
        } else if (!news.isEmpty()) {
            Flash.success(request, news.trim());
        }
        redirect(request, response, "/quiz/result?attempt=" + result.attemptId());
    }

    /** The quiz in progress, or {@code null} after redirecting to the quiz list if there is none. */
    private static QuizSession quizInProgress(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        QuizSession quizSession = (QuizSession) request.getSession().getAttribute(SessionKeys.QUIZ_SESSION);
        if (quizSession == null) {
            Flash.info(request, "You don't have a quiz in progress. Pick one to start!");
            redirect(request, response, "/quizzes");
        }
        return quizSession;
    }
}
