package com.quizzmosis.service;

/** Thrown when user input is invalid. The message is safe to show to the user. */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
