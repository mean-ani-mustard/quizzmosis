package com.quizzmosis.web.quiz;

import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizVisibility;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The quiz form (title, description, settings, visibility and time limit), shared by the "create"
 * and "edit" pages.
 */
final class QuizForm {

    private static final int MAX_TIME_LIMIT_MINUTES = 180;

    private QuizForm() {
    }

    /**
     * Copies the submitted form into {@code quiz}, even when something is wrong with it, so the
     * form can be shown again with what the user typed.
     *
     * @return what is wrong, to show above the form, or {@code null} if the quiz can be saved
     */
    static String read(HttpServletRequest request, Quiz quiz) {
        quiz.setTitle(trimmed(request, "title"));
        String description = trimmed(request, "description");
        quiz.setDescription(description.isEmpty() ? null : description);
        quiz.setRandomOrder(request.getParameter("randomOrder") != null);
        quiz.setMultiplePages(request.getParameter("multiplePages") != null);
        // Immediate correction only works when questions are shown one per page.
        quiz.setImmediateCorrection(quiz.isMultiplePages() && request.getParameter("immediateCorrection") != null);
        QuizVisibility visibility = QuizVisibility.fromParam(trimmed(request, "visibility"));
        quiz.setVisibility(visibility == null ? QuizVisibility.PUBLIC : visibility);
        String minutesText = trimmed(request, "timeLimitMinutes");
        Integer minutes = parseWholeNumber(minutesText);
        quiz.setTimeLimitSeconds(minutes == null ? null : minutes * 60);

        if (quiz.getTitle().isEmpty() || quiz.getTitle().length() > 100) {
            return "Please enter a title of at most 100 characters.";
        }
        if (description.length() > 500) {
            return "The description can be at most 500 characters long.";
        }
        if (!minutesText.isEmpty() && (minutes == null || minutes < 1 || minutes > MAX_TIME_LIMIT_MINUTES)) {
            return "The time limit must be a whole number of minutes from 1 to " + MAX_TIME_LIMIT_MINUTES
                    + ", or empty for no limit.";
        }
        return null;
    }

    private static Integer parseWholeNumber(String text) {
        try {
            return text.isEmpty() ? null : Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimmed(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
