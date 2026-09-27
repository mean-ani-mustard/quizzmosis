package com.quizzmosis.model;

/** Time windows the leaderboard can be filtered by. */
public enum Timeframe {

    ALL("all", "All time"),
    TODAY("today", "Today"),
    WEEK("week", "Last 7 days"),
    MONTH("month", "Last 30 days");

    private final String param;
    private final String label;

    Timeframe(String param, String label) {
        this.param = param;
        this.label = label;
    }

    public String getParam() {
        return param;
    }

    public String getLabel() {
        return label;
    }

    /** Reads the {@code period} request parameter; anything unknown means {@link #ALL}. */
    public static Timeframe fromParam(String value) {
        for (Timeframe timeframe : values()) {
            if (timeframe.param.equals(value)) {
                return timeframe;
            }
        }
        return ALL;
    }
}
