package com.quizzmosis.web.quiz;

import com.quizzmosis.dao.QuestionDAO;
import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.model.QuestionType;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.util.Format;
import com.quizzmosis.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The author's editor for a quiz: add, edit and delete questions, or delete the whole quiz.
 * {@code ?edit=<question id>} opens a question in the form for editing.
 */
@WebServlet("/quiz/questions")
public class ManageQuestionsServlet extends AuthorOnlyServlet {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_OPTION_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 1000;

    private final QuestionDAO questionDAO = new QuestionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = findOwnQuiz(request, response);
        if (quiz == null) {
            return;
        }
        Question editing = findOwnQuestion(quiz, intParam(request, "edit"));
        if (editing != null) {
            fillForm(request, editing);
        }
        show(request, response, quiz);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Quiz quiz = findOwnQuiz(request, response);
        if (quiz == null) {
            return;
        }
        String backToThisPage = "/quiz/questions?id=" + quiz.getQuizId();

        switch (param(request, "action")) {
            case "add" -> saveQuestion(request, response, quiz, null);
            case "update" -> {
                Question question = findOwnQuestion(quiz, intParam(request, "questionId"));
                if (question == null) {
                    redirect(request, response, backToThisPage);
                } else {
                    saveQuestion(request, response, quiz, question.getQuestionId());
                }
            }
            case "delete" -> {
                Question question = findOwnQuestion(quiz, intParam(request, "questionId"));
                if (question != null) {
                    questionDAO.delete(question.getQuestionId());
                    Flash.success(request, "Question deleted.");
                }
                redirect(request, response, backToThisPage);
            }
            case "delete_quiz" -> {
                quizDAO.delete(quiz.getQuizId());
                Flash.success(request, "Quiz \"" + quiz.getTitle() + "\" was deleted.");
                redirect(request, response, "/quizzes");
            }
            default -> redirect(request, response, backToThisPage);
        }
    }

    /** A question of this quiz, or {@code null} (also for a question id that belongs to another quiz). */
    private Question findOwnQuestion(Quiz quiz, Integer questionId) {
        Question question = questionId == null ? null : questionDAO.findById(questionId);
        return question != null && question.getQuizId() == quiz.getQuizId() ? question : null;
    }

    /** Adds a new question, or saves an edited one when {@code questionId} is set. */
    private void saveQuestion(HttpServletRequest request, HttpServletResponse response, Quiz quiz, Integer questionId)
            throws ServletException, IOException {
        QuestionType type = QuestionType.fromParam(param(request, "type"));
        Question question = new Question();
        question.setQuizId(quiz.getQuizId());
        question.setType(type);
        question.setQuestionText(param(request, "questionText"));
        if (type == QuestionType.MULTIPLE_CHOICE) {
            question.setChoices(readChoices(request));
        } else {
            question.setAcceptedAnswers(readAcceptedAnswers(request));
        }
        if (type == QuestionType.PICTURE_RESPONSE) {
            question.setImageUrl(param(request, "imageUrl"));
        }

        String error = validate(question);
        if (error != null) {
            // Show the form again with the error and with what the user typed, so nothing is lost.
            request.setAttribute("error", error);
            request.setAttribute("formType", param(request, "type"));
            request.setAttribute("formText", param(request, "questionText"));
            request.setAttribute("formImageUrl", param(request, "imageUrl"));
            request.setAttribute("formAnswers", param(request, "answers"));
            request.setAttribute("formChoices", readChoices(request));
            request.setAttribute("editingId", questionId);
            show(request, response, quiz);
            return;
        }

        if (questionId == null) {
            questionDAO.add(question);
            Flash.success(request, "Question added.");
        } else {
            question.setQuestionId(questionId);
            questionDAO.update(question);
            Flash.success(request, "Question saved.");
        }
        redirect(request, response, "/quiz/questions?id=" + quiz.getQuizId());
    }

    /** What's wrong with a question from the form, or {@code null} if it can be saved. */
    private static String validate(Question question) {
        QuestionType type = question.getType();
        if (type == null) {
            return "Please choose a question type.";
        }
        if (question.getQuestionText().isEmpty() || question.getQuestionText().length() > MAX_QUESTION_LENGTH) {
            return "Please enter the question (at most " + MAX_QUESTION_LENGTH + " characters).";
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            if (question.getChoices().size() < 2) {
                return "A multiple-choice question needs at least two options.";
            }
            if (question.getChoices().stream().noneMatch(Choice::isCorrect)) {
                return "Please tick at least one option as correct.";
            }
            return null;
        }
        if (question.getAcceptedAnswers().isEmpty()) {
            return "Please enter at least one accepted answer.";
        }
        if (type == QuestionType.PICTURE_RESPONSE && !isHttpUrl(question.getImageUrl())) {
            return "Picture questions need an image URL that starts with http:// or https://.";
        }
        return null;
    }

    /** Puts an existing question into the form, so it can be edited. */
    private static void fillForm(HttpServletRequest request, Question question) {
        request.setAttribute("formType", question.getType().getDbValue());
        request.setAttribute("formText", question.getQuestionText());
        request.setAttribute("formImageUrl", question.getImageUrl());
        request.setAttribute("formAnswers", String.join("\n", question.getAcceptedAnswers()));
        request.setAttribute("formChoices", question.getChoices());
        request.setAttribute("editingId", question.getQuestionId());
    }

    /** The options typed into the form. Checkbox "correctChoice" values are the positions of the correct ones. */
    private static List<Choice> readChoices(HttpServletRequest request) {
        String[] texts = request.getParameterValues("choiceText");
        String[] correctPositions = request.getParameterValues("correctChoice");
        // A HashSet, not Set.of(...), because Set.of throws on a duplicate value in a hand-made request.
        Set<String> correct = correctPositions == null ? Set.of() : new HashSet<>(Arrays.asList(correctPositions));
        List<Choice> choices = new ArrayList<>();
        if (texts != null) {
            for (int i = 0; i < texts.length; i++) {
                String text = texts[i].trim();
                if (!text.isEmpty()) {
                    choices.add(new Choice(Format.truncate(text, MAX_OPTION_LENGTH), correct.contains(String.valueOf(i))));
                }
            }
        }
        return choices;
    }

    /** One accepted answer per line; blank lines and duplicates are ignored. */
    private static List<String> readAcceptedAnswers(HttpServletRequest request) {
        Set<String> answers = new LinkedHashSet<>();
        Arrays.stream(param(request, "answers").split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .forEach(line -> answers.add(Format.truncate(line, MAX_OPTION_LENGTH)));
        return new ArrayList<>(answers);
    }

    /** Only http(s) links are allowed, so nobody can sneak a "javascript:" URL into an image tag. */
    private static boolean isHttpUrl(String url) {
        if (url.isEmpty() || url.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Quiz quiz)
            throws ServletException, IOException {
        request.setAttribute("quiz", quiz);
        request.setAttribute("questions", questionDAO.findByQuiz(quiz.getQuizId()));
        request.setAttribute("questionTypes", QuestionType.values());
        render(request, response, "quiz/questions");
    }
}
