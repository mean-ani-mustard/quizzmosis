<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Quizzes"/>
<c:set var="activeNav" value="quizzes"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 my-4">
        <h1 class="fw-bold display-6 mb-0">Quizzes</h1>
        <form method="get" action="${ctx}/quizzes" class="d-flex gap-2" role="search">
            <input type="search" name="q" class="form-control" placeholder="Search quizzes..."
                   value="${fn:escapeXml(query)}" aria-label="Search quizzes">
            <input type="hidden" name="sort" value="${sort.param}">
            <button type="submit" class="btn btn-light text-primary" aria-label="Search"><i class="bi bi-search"></i></button>
        </form>
        <a href="${ctx}/quiz/create" class="btn btn-warning fw-semibold"><i class="bi bi-plus-circle"></i> Create a quiz</a>
    </div>

    <c:if test="${not empty myQuizzes}">
        <section class="mb-5">
            <h2 class="h4 fw-semibold mb-3"><i class="bi bi-person-fill"></i> My quizzes</h2>
            <div class="row g-4">
                <c:forEach var="quiz" items="${myQuizzes}">
                    <%@ include file="/WEB-INF/fragments/quiz-card.jspf" %>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <section class="mb-5">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <h2 class="h4 fw-semibold mb-0">
                <c:choose>
                    <c:when test="${not empty query}">
                        Results for &ldquo;<c:out value="${query}"/>&rdquo;
                        <a href="${ctx}/quizzes?sort=${sort.param}" class="btn btn-sm btn-outline-light ms-2">Clear search</a>
                    </c:when>
                    <c:otherwise>All quizzes</c:otherwise>
                </c:choose>
            </h2>
            <%-- Sorting keeps the search; it starts again at page 1. --%>
            <div class="btn-group btn-group-sm" role="group" aria-label="Sort quizzes">
                <c:forEach var="option" items="${sorts}">
                    <c:url var="sortLink" value="/quizzes">
                        <c:if test="${not empty query}"><c:param name="q" value="${query}"/></c:if>
                        <c:param name="sort" value="${option.param}"/>
                    </c:url>
                    <a href="${sortLink}" class="btn ${option == sort ? 'btn-light' : 'btn-outline-light'}"
                       <c:if test="${option == sort}">aria-current="true"</c:if>>${option.label}</a>
                </c:forEach>
            </div>
        </div>

        <c:if test="${empty page.items}">
            <div class="glass-card p-4 text-center">
                <c:choose>
                    <c:when test="${not empty query}">No quizzes match your search.</c:when>
                    <c:otherwise>
                        <p>There are no quizzes yet. Be the first to create one!</p>
                        <a href="${ctx}/quiz/create" class="btn btn-light text-primary fw-semibold">Create a quiz</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <div class="row g-4">
            <c:forEach var="quiz" items="${page.items}">
                <%@ include file="/WEB-INF/fragments/quiz-card.jspf" %>
            </c:forEach>
        </div>

        <c:url var="pageLink" value="/quizzes">
            <c:if test="${not empty query}"><c:param name="q" value="${query}"/></c:if>
            <c:param name="sort" value="${sort.param}"/>
        </c:url>
        <%@ include file="/WEB-INF/fragments/pagination.jspf" %>
    </section>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
