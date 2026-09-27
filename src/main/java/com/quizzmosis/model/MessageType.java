package com.quizzmosis.model;

/**
 * Kinds of messages; {@code dbValue} is stored in {@code messages.message_type}. Notes are written
 * by users. The other three are sent automatically to tell a challenger what their friend did.
 */
public enum MessageType {
    NOTE("note"),
    CHALLENGE_ACCEPTED("challenge_accepted"),
    CHALLENGE_DECLINED("challenge_declined"),
    CHALLENGE_FINISHED("challenge_finished");

    private final String dbValue;

    MessageType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MessageType fromDbValue(String value) {
        for (MessageType type : values()) {
            if (type.dbValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + value);
    }
}
