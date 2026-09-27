package com.quizzmosis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A question of a quiz.
 *
 * <p>Multiple-choice questions use {@link #getChoices()}; the typed-answer types
 * (question-response, fill in the blank, picture-response) use {@link #getAcceptedAnswers()}.
 * Questions of the quiz someone is currently taking live in the HTTP session, hence Serializable.</p>
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private int questionId;
    private int quizId;
    private QuestionType type;
    private String questionText;
    private String imageUrl;
    private int questionOrder;
    private List<Choice> choices = new ArrayList<>();
    private List<String> acceptedAnswers = new ArrayList<>();

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers;
    }

    public boolean isMultipleChoice() {
        return type == QuestionType.MULTIPLE_CHOICE;
    }

    public boolean isPictureResponse() {
        return type == QuestionType.PICTURE_RESPONSE;
    }

    /** The correct answer(s) as readable text, e.g. "Paris" or "George Washington / Washington". */
    public String getCorrectAnswerText() {
        if (isMultipleChoice()) {
            return choices.stream()
                    .filter(Choice::isCorrect)
                    .map(Choice::getChoiceText)
                    .collect(Collectors.joining(" / "));
        }
        return String.join(" / ", acceptedAnswers);
    }
}
