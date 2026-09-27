<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${quiz.title}"/>
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

    <header class="glass-card p-4 mb-4">
        <div class="row align-items-center g-3">
            <div class="col-lg-8">
                <h1 class="display-6 fw-bold mb-2">
                    <i class="bi bi-lightbulb-fill text-warning me-2"></i><c:out value="${quiz.title}"/>
                </h1>
                <c:if test="${not empty quiz.description}">
                    <p class="lead mb-2 pre-wrap"><c:out value="${quiz.description}"/></p>
                </c:if>
                <p class="small mb-2">
                    <i class="bi bi-person-circle"></i> by
                    <a href="${ctx}/profile?id=${quiz.ownerId}" class="link-light fw-semibold"><c:out value="${quiz.ownerUsername}"/></a>
                    <span class="ms-3"><i class="bi bi-calendar3"></i> <fmt:formatDate value="${quiz.creationDate}" pattern="MMM d, yyyy"/></span>
                </p>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <%@ include file="/WEB-INF/fragments/rating-badge.jspf" %>
                    <%@ include file="/WEB-INF/fragments/visibility-badge.jspf" %>
                    <span class="badge text-bg-light">${quiz.questionCount} ${quiz.questionCount == 1 ? 'question' : 'questions'}</span>
                    <c:if test="${not empty quiz.timeLimitSeconds}">
                        <span class="badge bg-warning text-dark"><i class="bi bi-stopwatch"></i> ${quiz.timeLimitMinutes} min time limit</span>
                    </c:if>
                    <span class="badge text-bg-light">${quiz.multiplePages ? 'One question per page' : 'All questions on one page'}</span>
                    <c:if test="${quiz.randomOrder}"><span class="badge text-bg-light">Random order</span></c:if>
                    <c:if test="${quiz.immediateCorrection}"><span class="badge text-bg-light">Instant feedback</span></c:if>
                </div>
            </div>
            <div class="col-lg-4 text-lg-end d-flex flex-column gap-2 align-items-lg-end">
                <c:if test="${quiz.playable}">
                    <a href="${ctx}/quiz/start?id=${quiz.quizId}" class="btn btn-success btn-lg fw-semibold">
                        <i class="bi bi-play-fill"></i> Start quiz</a>
                    <%-- Nobody else can see an "Only me" quiz, so there is nobody to challenge. --%>
                    <c:if test="${quiz.visibility != 'PRIVATE'}">
                        <a href="${ctx}/quiz/challenge?id=${quiz.quizId}" class="btn btn-outline-light">
                            <i class="bi bi-flag"></i> Challenge a friend</a>
                    </c:if>
                </c:if>
                <c:if test="${isOwner}">
                    <a href="${ctx}/quiz/questions?id=${quiz.quizId}" class="btn btn-warning fw-semibold">
                        <i class="bi bi-pencil-square"></i> Edit quiz</a>
                </c:if>
                <c:if test="${not quiz.playable and not isOwner}">
                    <p class="mb-0 text-soft">This quiz doesn't have any questions yet.</p>
                </c:if>
                <c:if test="${canRate}">
                    <div id="rating" class="glass-inner px-3 py-2 mt-1">
                        <div class="small text-soft mb-1">${empty myRating ? 'Rate this quiz' : 'Your rating'}</div>
                        <%@ include file="/WEB-INF/fragments/rating-form.jspf" %>
                    </div>
                </c:if>
            </div>
        </div>
    </header>

    <section class="row g-3 mb-4">
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="stat-number">${stats.attemptCount}</div>
                <div class="stat-label">Plays</div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="stat-number">
                    <c:choose>
                        <c:when test="${stats.attemptCount > 0}"><fmt:formatNumber value="${stats.averagePercent}" maxFractionDigits="0"/>%</c:when>
                        <c:otherwise>&ndash;</c:otherwise>
                    </c:choose>
                </div>
                <div class="stat-label">Average score</div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="stat-number">${stats.attemptCount > 0 ? stats.formattedAverageDuration : '&ndash;'}</div>
                <div class="stat-label">Average time</div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="stat-number">${stats.attemptCount > 0 ? stats.highestScore : '&ndash;'}</div>
                <div class="stat-label">Best score</div>
            </div>
        </div>
    </section>

    <div class="row g-4">
        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
                    <h2 class="h5 fw-semibold mb-0"><i class="bi bi-person-check me-1"></i> Your attempts</h2>
                    <c:if test="${not empty myAttempts}">
                        <div class="btn-group btn-group-sm" role="group" aria-label="Sort your attempts">
                            <a href="${ctx}/quiz?id=${quiz.quizId}&amp;sort=date" class="btn ${sort == 'date' ? 'btn-light' : 'btn-outline-light'}">Newest</a>
                            <a href="${ctx}/quiz?id=${quiz.quizId}&amp;sort=score" class="btn ${sort == 'score' ? 'btn-light' : 'btn-outline-light'}">Best</a>
                            <a href="${ctx}/quiz?id=${quiz.quizId}&amp;sort=time" class="btn ${sort == 'time' ? 'btn-light' : 'btn-outline-light'}">Fastest</a>
                        </div>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${empty myAttempts}">
                        <p class="mb-0 text-soft">You haven't taken this quiz yet.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-glass table-sm align-middle">
                                <thead>
                                <tr><th>Date</th><th>Score</th><th>Time</th><th></th></tr>
                                </thead>
                                <tbody>
                                <c:forEach var="attempt" items="${myAttempts}">
                                    <tr>
                                        <td class="small text-nowrap"><fmt:formatDate value="${attempt.attemptDate}" pattern="MMM d, yyyy HH:mm"/></td>
                                        <td><strong>${attempt.score}/${attempt.totalQuestions}</strong> <span class="small text-soft">(${attempt.percentage}%)</span></td>
                                        <td>${attempt.formattedDuration}</td>
                                        <td class="text-end"><a href="${ctx}/quiz/result?attempt=${attempt.attemptId}" class="btn btn-sm btn-outline-light">Details</a></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-trophy-fill text-warning me-1"></i> Top players (all time)</h2>
                <c:choose>
                    <c:when test="${empty topAllTime}"><p class="mb-0 text-soft">Nobody has taken this quiz yet.</p></c:when>
                    <c:otherwise>
                        <c:set var="rows" value="${topAllTime}"/>
                        <c:set var="ranked" value="${true}"/>
                        <c:set var="showQuiz" value="${false}"/>
                        <%@ include file="/WEB-INF/fragments/attempt-table.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-calendar-day me-1"></i> Top players (last 24 hours)</h2>
                <c:choose>
                    <c:when test="${empty topLastDay}"><p class="mb-0 text-soft">No attempts in the last 24 hours.</p></c:when>
                    <c:otherwise>
                        <c:set var="rows" value="${topLastDay}"/>
                        <c:set var="ranked" value="${true}"/>
                        <c:set var="showQuiz" value="${false}"/>
                        <%@ include file="/WEB-INF/fragments/attempt-table.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-clock-history me-1"></i> Recent attempts</h2>
                <c:choose>
                    <c:when test="${empty recentAttempts}"><p class="mb-0 text-soft">No attempts yet.</p></c:when>
                    <c:otherwise>
                        <c:set var="rows" value="${recentAttempts}"/>
                        <c:set var="ranked" value="${false}"/>
                        <c:set var="showQuiz" value="${false}"/>
                        <%@ include file="/WEB-INF/fragments/attempt-table.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <%-- Comments are written on the results page after finishing the quiz (one per attempt). --%>
        <div class="col-12">
            <section id="comments" class="glass-card p-4">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-chat-quote-fill text-warning me-1"></i> Comments
                    <c:if test="${not empty comments}"><span class="badge text-bg-light align-middle">${fn:length(comments)}</span></c:if></h2>
                <c:if test="${empty comments}">
                    <p class="mb-0 text-soft">No comments yet. Finish the quiz to leave the first one.</p>
                </c:if>
                <c:forEach var="comment" items="${comments}">
                    <article class="glass-inner p-3 mb-2">
                        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-1">
                            <div>
                                <q:avatar name="${comment.displayName}" userId="${comment.userId}"/>
                                <a href="${ctx}/profile?id=${comment.userId}" class="link-light fw-semibold"><c:out value="${comment.displayName}"/></a>
                                <c:if test="${comment.score != null}">
                                    <span class="badge text-bg-light ms-1">${comment.score}/${comment.totalQuestions} (${comment.percentage}%)</span>
                                </c:if>
                                <small class="text-soft ms-2"><fmt:formatDate value="${comment.createdAt}" pattern="MMM d, yyyy"/></small>
                            </div>
                            <c:if test="${comment.userId == sessionScope.user.userId or sessionScope.user.admin}">
                                <form method="post" action="${ctx}/quiz/comment" class="m-0" data-confirm="Delete this comment?">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="commentId" value="${comment.commentId}">
                                    <button type="submit" class="btn btn-sm btn-outline-light" aria-label="Delete comment"><i class="bi bi-trash"></i></button>
                                </form>
                            </c:if>
                        </div>
                        <p class="mb-0 pre-wrap"><c:out value="${comment.content}"/></p>
                    </article>
                </c:forEach>
            </section>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
