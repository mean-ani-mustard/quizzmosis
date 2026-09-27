package com.quizzmosis.util;

import java.util.List;

/** Small formatting helpers shared by the model classes (and therefore the JSP pages). */
public final class Format {

    private Format() {
    }

    /** 75 seconds becomes "1:15". */
    public static String duration(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /** Score as a whole percentage of the total, e.g. 3 of 4 is 75. Returns 0 when there are no questions. */
    public static int percent(int score, int total) {
        return total <= 0 ? 0 : Math.round(score * 100f / total);
    }

    /** Cuts {@code text} to at most {@code maxLength} characters (null stays null). */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /** The initials shown in an avatar: "Alice Johnson" becomes "AJ", "bob" becomes "B". */
    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] words = name.trim().split("\\s+");
        String first = firstLetter(words[0]);
        return words.length == 1 ? first : first + firstLetter(words[words.length - 1]);
    }

    /** The first character, as a capital (whole code point, so an emoji isn't cut in half). */
    private static String firstLetter(String word) {
        return new String(Character.toChars(Character.toUpperCase(word.codePointAt(0))));
    }

    /** Joins names for a sentence: "Bob", "Bob and Carol", "Bob, Carol and Dave" ("" for none). */
    public static String names(List<String> names) {
        int count = names.size();
        if (count <= 1) {
            return count == 0 ? "" : names.get(0);
        }
        return String.join(", ", names.subList(0, count - 1)) + " and " + names.get(count - 1);
    }
}
