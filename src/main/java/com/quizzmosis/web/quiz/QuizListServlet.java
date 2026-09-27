package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuizDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSort;
import com.quizzmosis.util.Page;
import com.quizzmosis.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The quiz list, 20 quizzes per page: all quizzes the user may see, or the ones matching
 * {@code ?q=search text}, sorted by {@code ?sort=} (newest, most played or best rated).
 */
@WebServlet("/quizzes")
public class QuizListServlet extends BaseServlet {

    private static final int PAGE_SIZE = 20;

    private final QuizDAO quizDAO = new QuizDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = currentUser(request).getUserId();
        String query = param(request, "q");
        QuizSort sort = QuizSort.fromParam(param(request, "sort"));
        Page<Quiz> page = Page.load(intParam(request, "page"), PAGE_SIZE, quizDAO.countVisible(userId, query),
                (limit, offset) -> quizDAO.findVisible(userId, query, sort, limit, offset));

        request.setAttribute("query", query);
        request.setAttribute("sort", sort);
        request.setAttribute("sorts", QuizSort.values());
        request.setAttribute("page", page);
        if (query.isEmpty() && page.isFirst()) {
            request.setAttribute("myQuizzes", quizDAO.findByOwner(userId, userId));
        }
        render(request, response, "quiz/list");
    }
}
