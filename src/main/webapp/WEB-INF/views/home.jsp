<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Home"/>
<c:set var="activeNav" value="home"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <%-- Greeting --%>
    <header class="text-center py-5">
        <h1 class="display-5 fw-bold">Welcome back, <c:out value="${sessionScope.user.displayName}"/>!</h1>
        <div class="d-flex flex-wrap justify-content-center gap-3 my-3">
            <c:forEach var="achievement" items="${achievements}">
                <span data-bs-toggle="tooltip"
                      data-bs-title="${fn:escapeXml(achievement.name)}: ${fn:escapeXml(achievement.description)}">
                    <i class="bi ${achievement.icon} fs-2 text-warning"></i>
                </span>
            </c:forEach>
            <c:if test="${empty achievements}">
                <a href="${ctx}/achievements" class="link-light small">
                    <i class="bi bi-patch-check"></i> No achievements yet &ndash; see what you can earn
                </a>
            </c:if>
        </div>
        <p class="lead mb-4">What would you like to do today?</p>
        <div class="d-flex flex-wrap justify-content-center gap-2">
            <a href="${ctx}/quizzes" class="btn btn-light btn-lg text-primary fw-semibold">Browse quizzes</a>
            <a href="${ctx}/quiz/create" class="btn btn-outline-light btn-lg">Create a quiz</a>
        </div>
    </header>

    <%-- Announcements from administrators --%>
    <c:if test="${not empty announcements}">
        <section class="glass-card p-4 mb-5">
            <h2 class="h4 fw-semibold mb-3"><i class="bi bi-megaphone-fill me-2"></i>Announcements</h2>
            <c:forEach var="announcement" items="${announcements}">
                <div class="glass-inner p-3 mb-2">
                    <h3 class="h6 fw-semibold mb-1"><c:out value="${announcement.title}"/></h3>
                    <p class="mb-1 pre-wrap"><c:out value="${announcement.content}"/></p>
                    <small class="text-soft">
                        <i class="bi bi-calendar3 me-1"></i><fmt:formatDate value="${announcement.createdAt}" pattern="MMM d, yyyy"/>
                    </small>
                </div>
            </c:forEach>
        </section>
    </c:if>

    <section class="row g-4 mb-5">
        <div class="col-md-6">
            <div class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold"><i class="bi bi-pencil-square me-1"></i> Your quizzes</h2>
                <c:choose>
                    <c:when test="${empty myQuizzes}">
                        <p class="mb-0">You haven't created any quizzes yet.
                            <a href="${ctx}/quiz/create" class="link-light fw-semibold">Create one</a>!</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-unstyled mb-0 scroll-box">
                            <c:forEach var="quiz" items="${myQuizzes}">
                                <li class="mb-2">
                                    <i class="bi bi-lightbulb-fill text-warning me-1"></i>
                                    <a href="${ctx}/quiz?id=${quiz.quizId}" class="link-light fw-semibold"><c:out value="${quiz.title}"/></a>
                                    <%@ include file="/WEB-INF/fragments/visibility-badge.jspf" %>
                                    <span class="small text-soft ms-1">${quiz.questionCount} ${quiz.questionCount == 1 ? 'question' : 'questions'} &middot; ${quiz.attemptCount} ${quiz.attemptCount == 1 ? 'play' : 'plays'}</span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="col-md-6">
            <div class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold"><i class="bi bi-people-fill me-1"></i> Your friends</h2>
                <c:choose>
                    <c:when test="${empty friends}">
                        <p class="mb-0">No friends yet. <a href="${ctx}/friends" class="link-light fw-semibold">Find some</a>.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-inline mb-0 scroll-box">
                            <c:forEach var="friend" items="${friends}">
                                <li class="list-inline-item me-3 mb-2">
                                    <a href="${ctx}/profile?id=${friend.userId}" class="link-light person-link">
                                        <q:avatar name="${friend.displayName}" userId="${friend.userId}"/> <c:out value="${friend.displayName}"/>
                                    </a>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="col-12">
            <div class="glass-card p-4">
                <h2 class="h5 fw-semibold"><i class="bi bi-activity me-1"></i> Friends' recent activity</h2>
                <c:choose>
                    <c:when test="${empty friendsActivity}">
                        <p class="mb-0">No recent activity from your friends.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-unstyled mb-0 scroll-box">
                            <c:forEach var="activity" items="${friendsActivity}">
                                <li class="mb-2">
                                    <a href="${ctx}/profile?id=${activity.userId}" class="link-light fw-semibold"><c:out value="${activity.displayName}"/></a>
                                    scored <strong>${activity.score}/${activity.totalQuestions}</strong> (${activity.percentage}%) on
                                    <a href="${ctx}/quiz?id=${activity.quizId}" class="link-light"><c:out value="${activity.quizTitle}"/></a>
                                    <small class="text-soft">&middot; <fmt:formatDate value="${activity.attemptDate}" pattern="MMM d, HH:mm"/></small>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </section>

    <c:if test="${not empty popularQuizzes}">
        <section class="mb-5">
            <h2 class="h4 fw-semibold mb-3"><i class="bi bi-fire text-warning"></i> Popular quizzes</h2>
            <div class="row g-4">
                <c:forEach var="quiz" items="${popularQuizzes}">
                    <%@ include file="/WEB-INF/fragments/quiz-card.jspf" %>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <c:if test="${not empty recentQuizzes}">
        <section class="mb-5">
            <h2 class="h4 fw-semibold mb-3"><i class="bi bi-stars text-warning"></i> Recently created</h2>
            <div class="row g-4">
                <c:forEach var="quiz" items="${recentQuizzes}">
                    <%@ include file="/WEB-INF/fragments/quiz-card.jspf" %>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <section class="mb-5">
        <h2 class="h4 fw-semibold mb-3"><i class="bi bi-clock-history"></i> Your recent results</h2>
        <div class="glass-card p-4">
            <c:choose>
                <c:when test="${empty myRecentAttempts}">
                    <p class="mb-3">You haven't taken any quizzes yet.</p>
                    <a href="${ctx}/quizzes" class="btn btn-light text-primary fw-semibold"><i class="bi bi-play-fill"></i> Start playing</a>
                </c:when>
                <c:otherwise>
                    <c:forEach var="attempt" items="${myRecentAttempts}">
                        <div class="glass-inner p-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2">
                            <div>
                                <a href="${ctx}/quiz?id=${attempt.quizId}" class="link-light fw-semibold"><c:out value="${attempt.quizTitle}"/></a>
                                <div class="small text-soft"><fmt:formatDate value="${attempt.attemptDate}" pattern="MMM d, yyyy 'at' HH:mm"/></div>
                            </div>
                            <a href="${ctx}/quiz/result?attempt=${attempt.attemptId}" class="badge bg-success fs-6 text-decoration-none">
                                <i class="bi bi-trophy-fill me-1"></i>${attempt.score}/${attempt.totalQuestions} &middot; ${attempt.percentage}%
                            </a>
                        </div>
                    </c:forEach>
                    <div class="text-center mt-3">
                        <a href="${ctx}/history" class="btn btn-outline-light btn-sm"><i class="bi bi-clock-history me-1"></i>Full history</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
