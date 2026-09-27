package com.quizzmosis.model;

import java.io.Serializable;

/** One option of a multiple-choice question. */
public class Choice implements Serializable {

    private static final long serialVersionUID = 1L;

    private int choiceId;
    private int questionId;
    private String choiceText;
    private boolean correct;

    public Choice() {
    }

    public Choice(String choiceText, boolean correct) {
        this.choiceText = choiceText;
        this.correct = correct;
    }

    public int getChoiceId() {
        return choiceId;
    }

    public void setChoiceId(int choiceId) {
        this.choiceId = choiceId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getChoiceText() {
        return choiceText;
    }

    public void setChoiceText(String choiceText) {
        this.choiceText = choiceText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
