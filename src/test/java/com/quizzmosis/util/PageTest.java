package com.quizzmosis.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTest {

    /** The numbers 1 to 45, served a page at a time like a database would. */
    private static final List<Integer> NUMBERS = IntStream.rangeClosed(1, 45).boxed().toList();

    private static Page<Integer> page(Integer requested) {
        return Page.load(requested, 20, NUMBERS.size(),
                (limit, offset) -> NUMBERS.subList(offset, Math.min(offset + limit, NUMBERS.size())));
    }

    @Test
    void pagesSplitTheListIntoEqualParts() {
        Page<Integer> first = page(1);
        assertEquals(20, first.getItems().size());
        assertEquals(1, first.getItems().get(0));
        assertEquals(3, first.getTotalPages());
        assertTrue(first.isFirst());
        assertFalse(first.isLast());

        Page<Integer> last = page(3);
        assertEquals(List.of(41, 42, 43, 44, 45), last.getItems());
        assertTrue(last.isLast());
    }

    @Test
    void missingOrImpossiblePageNumbersShowTheNearestPage() {
        assertEquals(1, page(null).getNumber());
        assertEquals(1, page(-4).getNumber());
        assertEquals(3, page(99).getNumber());
    }

    @Test
    void anEmptyListIsOneEmptyPage() {
        Page<Integer> empty = Page.load(5, 20, 0, (limit, offset) -> List.of());

        assertEquals(1, empty.getNumber());
        assertEquals(1, empty.getTotalPages());
        assertTrue(empty.getItems().isEmpty());
        assertTrue(empty.isFirst() && empty.isLast());
    }
}
