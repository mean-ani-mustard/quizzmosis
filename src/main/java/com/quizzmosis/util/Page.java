package com.quizzmosis.util;

import java.util.List;

/**
 * One page of a longer list (for example quizzes 21 to 40) with what the Previous and Next
 * buttons need to know. Pages are numbered from 1.
 */
public final class Page<T> {

    /** Loads one page of items: at most {@code limit} of them, after skipping the first {@code offset}. */
    @FunctionalInterface
    public interface Loader<T> {
        List<T> load(int limit, int offset);
    }

    private final List<T> items;
    private final int number;
    private final int totalPages;

    private Page(List<T> items, int number, int totalPages) {
        this.items = items;
        this.number = number;
        this.totalPages = totalPages;
    }

    /**
     * Loads the requested page. A missing or impossible page number (e.g. ?page=99 when there are
     * only 3 pages) shows the nearest page that exists instead of an empty list.
     */
    public static <T> Page<T> load(Integer requested, int size, int totalItems, Loader<T> loader) {
        int totalPages = Math.max(1, (totalItems + size - 1) / size);
        int number = requested == null ? 1 : Math.min(Math.max(requested, 1), totalPages);
        return new Page<>(loader.load(size, (number - 1) * size), number, totalPages);
    }

    public List<T> getItems() {
        return items;
    }

    public int getNumber() {
        return number;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return number == 1;
    }

    public boolean isLast() {
        return number == totalPages;
    }
}
