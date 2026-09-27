package com.quizzmosis.service;

import com.quizzmosis.model.AttemptAnswer;
import com.quizzmosis.model.Choice;
import com.quizzmosis.model.Question;
import com.quizzmosis.util.Format;

import java.util.Locale;

/** Decides whether an answer is correct. Pure logic without database access, so it is easy to unit test. */
public final class QuizGrader {

    /** Matches the length of the answer columns in the database. */
    private static final int MAX_ANSWER_LENGTH = 500;

    private QuizGrader() {
    }

    /**
     * Grades one response.
     *
     * @param response for multiple choice the id of the chosen option, otherwise the typed text
     */
    public static AttemptAnswer grade(Question question, String response) {
        AttemptAnswer answer = new AttemptAnswer();
        answer.setQuestionId(question.getQuestionId());
        answer.setQuestionText(question.getQuestionText());
        answer.setCorrectAnswer(Format.truncate(question.getCorrectAnswerText(), MAX_ANSWER_LENGTH));

        if (question.isMultipleChoice()) {
            Choice chosen = findChoice(question, response);
            answer.setUserAnswer(chosen == null ? null : chosen.getChoiceText());
            answer.setCorrect(chosen != null && chosen.isCorrect());
        } else {
            String typed = response == null ? "" : response.trim();
            answer.setUserAnswer(typed.isEmpty() ? null : Format.truncate(typed, MAX_ANSWER_LENGTH));
            String normalized = normalize(typed);
            boolean correct = !normalized.isEmpty() && question.getAcceptedAnswers().stream()
                    .anyMatch(accepted -> normalize(accepted).equals(normalized));
            answer.setCorrect(correct);
        }
        return answer;
    }

    /**
     * Only the options of <em>this</em> question count. The first version looked the choice up by
     * id alone, so posting the id of a correct option from another question scored a point.
     */
    private static Choice findChoice(Question question, String response) {
        if (response == null) {
            return null;
        }
        try {
            int choiceId = Integer.parseInt(response.trim());
            for (Choice choice : question.getChoices()) {
                if (choice.getChoiceId() == choiceId) {
                    return choice;
                }
            }
        } catch (NumberFormatException e) {
            // Not a number: treated like no answer.
        }
        return null;
    }

    /** Case-insensitive and ignores extra spaces: "  George   WASHINGTON " equals "george washington". */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
