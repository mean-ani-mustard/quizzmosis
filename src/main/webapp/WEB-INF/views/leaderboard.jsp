<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Leaderboard"/>
<c:set var="activeNav" value="leaderboard"/>
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
        <h1 class="display-6 fw-bold"><i class="bi bi-trophy-fill text-warning me-2"></i>Leaderboard</h1>
        <p class="lead mb-0">Everyone's best result per quiz, ranked by percentage.</p>
    </header>

    <div class="glass-card p-4">
        <form method="get" action="${ctx}/leaderboard" class="row g-3 mb-4 align-items-end">
            <div class="col-md-6">
                <label for="quiz" class="form-label">Quiz</label>
                <select class="form-select" id="quiz" name="quiz" onchange="this.form.submit()">
                    <option value="">All quizzes</option>
                    <c:forEach var="quiz" items="${quizzes}">
                        <option value="${quiz.quizId}" ${quiz.quizId == selectedQuiz ? 'selected' : ''}><c:out value="${quiz.title}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-4">
                <label for="period" class="form-label">Period</label>
                <select class="form-select" id="period" name="period" onchange="this.form.submit()">
                    <c:forEach var="option" items="${periods}">
                        <option value="${option.param}" ${option == period ? 'selected' : ''}>${option.label}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-light text-primary w-100">Show</button>
            </div>
        </form>

        <c:choose>
            <c:when test="${empty entries}">
                <div class="text-center py-4">
                    <i class="bi bi-inbox display-5"></i>
                    <p class="mt-2">No results for this selection yet.</p>
                    <a href="${ctx}/quizzes" class="btn btn-light text-primary"><i class="bi bi-play-fill"></i> Take a quiz</a>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="rows" value="${entries}"/>
                <c:set var="ranked" value="${true}"/>
                <c:set var="showQuiz" value="${true}"/>
                <%@ include file="/WEB-INF/fragments/attempt-table.jspf" %>
            </c:otherwise>
        </c:choose>

        <c:if test="${not empty personalBest}">
            <div class="glass-inner p-3 mt-4">
                <i class="bi bi-star-fill text-warning me-1"></i>
                <strong>Your best:</strong>
                ${personalBest.score}/${personalBest.totalQuestions} (${personalBest.percentage}%) on
                <a href="${ctx}/quiz?id=${personalBest.quizId}" class="link-light"><c:out value="${personalBest.quizTitle}"/></a>
                in ${personalBest.formattedDuration}
                <span class="small text-soft">&middot; <fmt:formatDate value="${personalBest.attemptDate}" pattern="MMM d, yyyy"/></span>
            </div>
        </c:if>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
