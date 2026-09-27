<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="History"/>
<c:set var="activeNav" value="history"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <header class="text-center py-4">
        <h1 class="display-6 fw-bold">Your quiz history</h1>
        <p class="lead mb-0">Every quiz you've finished, newest first.</p>
    </header>

    <div class="glass-card p-4">
        <c:choose>
            <c:when test="${empty page.items}">
                <div class="text-center py-4">
                    <i class="bi bi-clock-history display-5"></i>
                    <p class="mt-2">You haven't finished any quizzes yet.</p>
                    <a href="${ctx}/quizzes" class="btn btn-light text-primary fw-semibold"><i class="bi bi-play-fill"></i> Browse quizzes</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-glass align-middle">
                        <thead>
                        <tr>
                            <th scope="col">Quiz</th>
                            <th scope="col">Date</th>
                            <th scope="col">Score</th>
                            <th scope="col">Time</th>
                            <th scope="col"></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="attempt" items="${page.items}">
                            <tr>
                                <td><a href="${ctx}/quiz?id=${attempt.quizId}" class="link-light fw-semibold"><c:out value="${attempt.quizTitle}"/></a></td>
                                <td class="small text-nowrap"><fmt:formatDate value="${attempt.attemptDate}" pattern="MMM d, yyyy 'at' HH:mm"/></td>
                                <td>
                                    <span class="badge ${attempt.percentage >= 80 ? 'bg-success' : attempt.percentage >= 50 ? 'bg-warning text-dark' : 'bg-danger'}">
                                        ${attempt.score}/${attempt.totalQuestions} &middot; ${attempt.percentage}%
                                    </span>
                                </td>
                                <td>${attempt.formattedDuration}</td>
                                <td class="text-end"><a href="${ctx}/quiz/result?attempt=${attempt.attemptId}" class="btn btn-sm btn-outline-light">Details</a></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <c:url var="pageLink" value="/history"/>
                <%@ include file="/WEB-INF/fragments/pagination.jspf" %>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
