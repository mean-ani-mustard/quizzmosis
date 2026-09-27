<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Error page for 403, 404 and 500 (see web.xml). For 403/404 it shows the message the servlet
    passed to response.sendError(...). For 500 it deliberately shows no technical details: the
    exception is in the server log, but a stack trace in the browser would help attackers.
--%>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="pageTitle" value="${status == 404 ? 'Not found' : status == 403 ? 'Not allowed' : 'Error'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-5">
    <div class="glass-card p-5 text-center mx-auto" style="max-width: 620px;">
        <c:choose>
            <c:when test="${status == 404}">
                <i class="bi bi-signpost-split display-3 text-warning"></i>
                <h1 class="h2 fw-bold mt-3">Page not found</h1>
            </c:when>
            <c:when test="${status == 403}">
                <i class="bi bi-shield-lock display-3 text-warning"></i>
                <h1 class="h2 fw-bold mt-3">You can't do that</h1>
            </c:when>
            <c:otherwise>
                <i class="bi bi-exclamation-triangle-fill display-3 text-warning"></i>
                <h1 class="h2 fw-bold mt-3">Something went wrong</h1>
            </c:otherwise>
        </c:choose>

        <p class="lead mb-4">
            <c:choose>
                <c:when test="${(status == 403 or status == 404) and not empty requestScope['jakarta.servlet.error.message']}">
                    <c:out value="${requestScope['jakarta.servlet.error.message']}"/>
                </c:when>
                <c:when test="${status == 404}">We couldn't find the page you were looking for.</c:when>
                <c:when test="${status == 403}">You don't have permission to see this page.</c:when>
                <c:otherwise>An unexpected error occurred. Please try again in a moment.</c:otherwise>
            </c:choose>
        </p>
        <a href="${ctx}/home" class="btn btn-light btn-lg text-primary fw-semibold"><i class="bi bi-house-door-fill me-1"></i> Go home</a>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
