package com.quizzmosis.model;

/** Where a challenge stands. {@code dbValue} is what is stored in {@code challenges.status}. */
public enum ChallengeStatus {
    /** Waiting for the challenged friend to accept or decline. */
    PENDING("pending", "Waiting"),
    /** Accepted; becomes COMPLETED when the friend finishes the quiz. */
    ACCEPTED("accepted", "Accepted"),
    DECLINED("declined", "Declined"),
    COMPLETED("completed", "Completed");

    private final String dbValue;
    private final String label;

    ChallengeStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static ChallengeStatus fromDbValue(String value) {
        for (ChallengeStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown challenge status: " + value);
    }
}
