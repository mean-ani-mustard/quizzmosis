package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.User;
import com.quizzmosis.service.ChallengeService;
import com.quizzmosis.service.QuizAccess;
import com.quizzmosis.service.ValidationException;
import com.quizzmosis.web.BaseServlet;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Challenge a friend to take a quiz; the challenge arrives in their messages. */
@WebServlet("/quiz/challenge")
public class ChallengeServlet extends BaseServlet {

    private final QuizDAO quizDAO = new QuizDAO();
    private final FriendsDAO friendsDAO = new FriendsDAO();
    private final ChallengeService challengeService = new ChallengeService();
    private final UserDAO userDAO = new UserDAO();
    private final QuizAccess quizAccess = new QuizAccess();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = findQuiz(request, response);
        if (quiz == null) {
            return;
        }
        List<User> friends = friendsDAO.findFriends(currentUser(request).getUserId());
        request.setAttribute("quiz", quiz);
        request.setAttribute("hasFriends", !friends.isEmpty());
        // Only friends who can see the quiz can be challenged to it.
        request.setAttribute("friends", friends.stream()
                .filter(friend -> quizAccess.isVisibleTo(quiz, friend.getUserId()))
                .toList());
        render(request, response, "quiz/challenge");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Quiz quiz = findQuiz(request, response);
        if (quiz == null) {
            return;
        }
        int userId = currentUser(request).getUserId();
        Integer friendId = intParam(request, "friendId");
        User friend = friendId == null ? null : userDAO.findById(friendId);
        if (friend == null) {
            Flash.error(request, "Please choose a friend.");
            redirect(request, response, "/quiz/challenge?id=" + quiz.getQuizId());
            return;
        }
        try {
            challengeService.send(userId, friend.getUserId(), quiz, param(request, "message"));
        } catch (ValidationException e) {
            Flash.error(request, e.getMessage());
            redirect(request, response, "/quiz/challenge?id=" + quiz.getQuizId());
            return;
        }
        Flash.success(request, "Challenge sent to " + friend.getDisplayName() + "!");
        redirect(request, response, "/quiz?id=" + quiz.getQuizId());
    }

    private Quiz findQuiz(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer quizId = intParam(request, "id");
        Quiz quiz = quizId == null ? null : quizDAO.findById(quizId);
        if (quiz == null || !quizAccess.canOpen(quiz, currentUser(request))) {
            notFound(response, "That quiz doesn't exist (any more).");
            return null;
        }
        return quiz;
    }
}
