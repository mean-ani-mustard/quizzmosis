package com.quizzmosis.web;

/** Names of the attributes stored in the HTTP session, in one place so they can't be mistyped. */
public final class SessionKeys {

    /** The logged-in {@link com.quizzmosis.model.User}. JSPs read it as {@code sessionScope.user}. */
    public static final String USER = "user";

    /** The {@link com.quizzmosis.model.QuizSession} of the quiz being taken right now. */
    public static final String QUIZ_SESSION = "quizSession";

    /** A one-time {@link Flash} message shown on the next page. */
    public static final String FLASH = "flash";

    /** The secret token every POST form must send back (see CsrfFilter). */
    public static final String CSRF_TOKEN = "csrfToken";

    private SessionKeys() {
    }
}
