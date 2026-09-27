package com.quizzmosis.model;

/**
 * Who can see a quiz. {@code dbValue} is what is stored in the {@code quizzes.visibility} column.
 * The rule itself lives in {@link com.quizzmosis.service.QuizAccess} (for one quiz) and in
 * {@code QuizDAO.VISIBLE_TO} (for lists of quizzes).
 */
public enum QuizVisibility {
    PUBLIC("public", "Public", "Everyone can see and play it.", "bi-globe2"),
    FRIENDS("friends", "Friends only", "Only you and your friends can see and play it.", "bi-people-fill"),
    PRIVATE("private", "Only me", "Only you can see and play it.", "bi-lock-fill");

    private final String dbValue;
    private final String label;
    private final String description;
    /** Bootstrap Icons class shown next to the label. */
    private final String icon;

    QuizVisibility(String dbValue, String label, String description, String icon) {
        this.dbValue = dbValue;
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    /** Converts a database value back into the enum. */
    public static QuizVisibility fromDbValue(String value) {
        QuizVisibility visibility = fromParam(value);
        if (visibility == null) {
            throw new IllegalArgumentException("Unknown quiz visibility: " + value);
        }
        return visibility;
    }

    /** Lenient version for request parameters: returns {@code null} for unknown values. */
    public static QuizVisibility fromParam(String value) {
        for (QuizVisibility visibility : values()) {
            if (visibility.dbValue.equals(value)) {
                return visibility;
            }
        }
        return null;
    }
}
