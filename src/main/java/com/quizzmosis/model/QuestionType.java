package com.quizzmosis.model;

/**
 * The four kinds of questions a quiz can contain. {@code dbValue} is what is stored in the
 * {@code questions.question_type} column.
 */
public enum QuestionType {

    QUESTION_RESPONSE("question_response", "Question-Response"),
    FILL_BLANK("fill_blank", "Fill in the Blank"),
    MULTIPLE_CHOICE("multiple_choice", "Multiple Choice"),
    PICTURE_RESPONSE("picture_response", "Picture-Response");

    private final String dbValue;
    private final String label;

    QuestionType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    /** Converts a database value back into the enum. */
    public static QuestionType fromDbValue(String value) {
        QuestionType type = fromParam(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown question type: " + value);
        }
        return type;
    }

    /** Lenient version for request parameters: returns {@code null} for unknown values. */
    public static QuestionType fromParam(String value) {
        for (QuestionType type : values()) {
            if (type.dbValue.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
