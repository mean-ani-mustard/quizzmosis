<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Results: ${quiz.title}"/>
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

    <div class="glass-card p-4 p-md-5 mx-auto quiz-panel">
        <div class="text-center mb-4">
            <i class="bi bi-trophy-fill text-warning display-5"></i>
            <h1 class="h3 fw-bold mt-2">Results: <c:out value="${quiz.title}"/></h1>
            <p class="text-soft small mb-0"><fmt:formatDate value="${attempt.attemptDate}" pattern="MMM d, yyyy 'at' HH:mm"/></p>
            <c:if test="${not empty challengerNames}">
                <p class="mt-2 mb-0"><i class="bi bi-flag-fill text-warning"></i>
                    This attempt answered a challenge: <strong><c:out value="${challengerNames}"/></strong> got your score.</p>
            </c:if>
        </div>

        <div class="row g-3 text-center mb-4">
            <div class="col-4">
                <div class="stat-card">
                    <div class="stat-number">${attempt.score}/${attempt.totalQuestions}</div>
                    <div class="stat-label">Score</div>
                </div>
            </div>
            <div class="col-4">
                <div class="stat-card">
                    <div class="stat-number">${attempt.percentage}%</div>
                    <div class="stat-label">Correct</div>
                </div>
            </div>
            <div class="col-4">
                <div class="stat-card">
                    <div class="stat-number">${attempt.formattedDuration}</div>
                    <div class="stat-label">Time</div>
                </div>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty answers}">
                <p class="text-center text-soft">Detailed answers aren't available for this attempt.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-glass align-middle">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">Question</th>
                            <th scope="col">Your answer</th>
                            <th scope="col">Correct answer</th>
                            <th scope="col" class="text-center">Result</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="answer" items="${answers}" varStatus="number">
                            <tr>
                                <td>${number.index + 1}</td>
                                <td><c:out value="${answer.questionText}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty answer.userAnswer}"><span class="text-soft fst-italic">no answer</span></c:when>
                                        <c:otherwise><c:out value="${answer.userAnswer}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="small"><c:out value="${answer.correctAnswer}"/></td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${answer.correct}"><span class="badge bg-success"><i class="bi bi-check-lg"></i> Correct</span></c:when>
                                        <c:otherwise><span class="badge bg-danger"><i class="bi bi-x-lg"></i> Wrong</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>

        <%-- Rating the quiz (players can change their rating later). --%>
        <c:if test="${canRate}">
            <section id="rating" class="glass-inner p-3 mt-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                <h2 class="h6 fw-semibold mb-0"><i class="bi bi-star-fill text-warning"></i>
                    ${empty myRating ? 'How many stars does this quiz get?' : 'Your rating of this quiz'}</h2>
                <c:set var="ratingAttemptId" value="${attempt.attemptId}"/>
                <%@ include file="/WEB-INF/fragments/rating-form.jspf" %>
            </section>
        </c:if>

        <%-- A comment on the quiz: one per attempt, shown on the quiz page next to the score. --%>
        <c:if test="${not empty comment or canComment}">
            <section id="comment" class="glass-inner p-3 mt-4">
                <c:choose>
                    <c:when test="${not empty comment}">
                        <h2 class="h6 fw-semibold"><i class="bi bi-chat-quote-fill text-warning"></i> Your comment</h2>
                        <p class="mb-2 pre-wrap"><c:out value="${comment.content}"/></p>
                        <a href="${ctx}/quiz?id=${quiz.quizId}#comments" class="small link-light">See all comments on this quiz</a>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/quiz/comment" class="m-0">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="attemptId" value="${attempt.attemptId}">
                            <label for="commentText" class="form-label h6 fw-semibold">
                                <i class="bi bi-chat-quote-fill text-warning"></i> What did you think of this quiz?</label>
                            <textarea id="commentText" name="content" class="form-control mb-2" rows="2" maxlength="1000" required
                                      placeholder="Too easy? Loved it? Leave a comment for other players..."></textarea>
                            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                                <small class="text-soft">
                                    Shown on the quiz page with your score.
                                    <c:if test="${not empty challengerNames}"><c:out value="${challengerNames}"/> will see it too.</c:if>
                                </small>
                                <button type="submit" class="btn btn-light text-primary"><i class="bi bi-send"></i> Post comment</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>

        <div class="d-flex flex-wrap justify-content-center gap-2 mt-4">
            <a href="${ctx}/quiz/start?id=${quiz.quizId}" class="btn btn-light text-primary fw-semibold">
                <i class="bi bi-arrow-repeat"></i> Try again</a>
            <a href="${ctx}/quiz?id=${quiz.quizId}" class="btn btn-outline-light">
                <i class="bi bi-bar-chart"></i> Quiz statistics</a>
            <%-- Nobody else can see an "Only me" quiz, so there is nobody to challenge. --%>
            <c:if test="${quiz.visibility != 'PRIVATE'}">
                <a href="${ctx}/quiz/challenge?id=${quiz.quizId}" class="btn btn-outline-light">
                    <i class="bi bi-flag"></i> Challenge a friend</a>
            </c:if>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
