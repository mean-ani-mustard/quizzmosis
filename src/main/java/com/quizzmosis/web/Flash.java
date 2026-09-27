package com.quizzmosis.web;

import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;

/**
 * A message that survives one redirect ("flash" message), e.g. "Quiz created!".
 *
 * <p>After a POST the servlets redirect instead of rendering a page (Post/Redirect/Get), so that
 * refreshing the page doesn't submit the form twice. The message is parked in the session and
 * {@code fragments/flash.jspf} shows it on the next page and then removes it.</p>
 */
public class Flash implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Bootstrap alert style: success, danger, warning or info. */
    private final String type;
    private final String message;

    private Flash(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public static void success(HttpServletRequest request, String message) {
        set(request, "success", message);
    }

    public static void error(HttpServletRequest request, String message) {
        set(request, "danger", message);
    }

    public static void warning(HttpServletRequest request, String message) {
        set(request, "warning", message);
    }

    public static void info(HttpServletRequest request, String message) {
        set(request, "info", message);
    }

    private static void set(HttpServletRequest request, String type, String message) {
        request.getSession().setAttribute(SessionKeys.FLASH, new Flash(type, message));
    }
}
