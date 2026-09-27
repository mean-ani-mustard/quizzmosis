package com.quizzmosis.model;

/** The orders the quiz list can be sorted in; {@code param} is the value in the address (?sort=...). */
public enum QuizSort {
    NEWEST("newest", "Newest"),
    POPULAR("popular", "Most played"),
    RATED("rated", "Best rated");

    private final String param;
    private final String label;

    QuizSort(String param, String label) {
        this.param = param;
        this.label = label;
    }

    public String getParam() {
        return param;
    }

    public String getLabel() {
        return label;
    }

    /** The sort order for a ?sort= value; anything unknown means newest first. */
    public static QuizSort fromParam(String value) {
        for (QuizSort sort : values()) {
            if (sort.param.equals(value)) {
                return sort;
            }
        }
        return NEWEST;
    }
}
