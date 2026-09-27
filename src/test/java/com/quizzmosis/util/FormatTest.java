package com.quizzmosis.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormatTest {

    @Test
    void durationIsShownAsMinutesAndSeconds() {
        assertEquals("0:00", Format.duration(0));
        assertEquals("0:09", Format.duration(9));
        assertEquals("1:15", Format.duration(75));
        assertEquals("61:01", Format.duration(3661));
        assertEquals("0:00", Format.duration(-5), "negative durations are treated as 0");
    }

    @Test
    void percentIsRoundedToAWholeNumber() {
        assertEquals(75, Format.percent(3, 4));
        assertEquals(67, Format.percent(2, 3));
        assertEquals(33, Format.percent(1, 3));
        assertEquals(100, Format.percent(5, 5));
        assertEquals(0, Format.percent(0, 0), "a quiz without questions must not divide by zero");
    }

    @Test
    void truncateOnlyShortensLongText() {
        assertEquals("abc", Format.truncate("abcdef", 3));
        assertEquals("abc", Format.truncate("abc", 3));
        assertEquals("", Format.truncate("", 3));
        assertNull(Format.truncate(null, 3));
    }

    @Test
    void initialsComeFromTheFirstAndLastWord() {
        assertEquals("AJ", Format.initials("Alice Johnson"));
        assertEquals("AL", Format.initials("  anna   maria  lopez "));
        assertEquals("B", Format.initials("bob"));
        assertEquals("?", Format.initials("   "));
        assertEquals("?", Format.initials(null));
        assertEquals("\uD83D\uDE00", Format.initials("\uD83D\uDE00 happy").substring(0, 2), "an emoji isn't cut in half");
    }

    @Test
    void namesAreJoinedLikeInASentence() {
        assertEquals("", Format.names(List.of()));
        assertEquals("Bob", Format.names(List.of("Bob")));
        assertEquals("Bob and Carol", Format.names(List.of("Bob", "Carol")));
        assertEquals("Bob, Carol and Dave", Format.names(List.of("Bob", "Carol", "Dave")));
    }
}
