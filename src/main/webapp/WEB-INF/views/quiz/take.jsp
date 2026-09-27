<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="${quizSession.quizTitle}"/>
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

    <div class="glass-card p-4 p-md-5 mx-auto quiz-panel">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
            <h1 class="h4 fw-bold mb-0"><c:out value="${quizSession.quizTitle}"/></h1>
            <%-- With a time limit, app.js counts down and sends the page's form when time runs out. --%>
            <c:if test="${not empty secondsLeft}">
                <span class="badge bg-light text-primary fs-6" id="quizTimer" data-seconds-left="${secondsLeft}" role="timer">
                    <i class="bi bi-stopwatch"></i> <span data-timer-text>${timeLeft}</span></span>
            </c:if>
            <c:if test="${quizSession.multiplePages}">
                <div class="d-flex align-items-center gap-2 small">
                    <span>${quizSession.answeredCount} of ${quizSession.totalQuestions} answered</span>
                    <div class="progress" style="width: 140px; height: 8px;" role="progressbar"
                         aria-valuenow="${quizSession.answeredCount}" aria-valuemin="0" aria-valuemax="${quizSession.totalQuestions}">
                        <div class="progress-bar bg-warning"
                             style="width: ${quizSession.answeredCount * 100 / quizSession.totalQuestions}%"></div>
                    </div>
                </div>
            </c:if>
        </div>

        <c:choose>
            <%-- 0) The time limit is over (e.g. the page was reloaded late) --%>
            <c:when test="${timeUp}">
                <div class="text-center py-3">
                    <i class="bi bi-stopwatch display-5 text-warning"></i>
                    <p class="lead mt-2">Time's up! Questions you didn't answer count as wrong.</p>
                    <form method="post" action="${ctx}/quiz/take">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="quizId" value="${quizSession.quizId}">
                        <input type="hidden" name="action" value="finish">
                        <button type="submit" class="btn btn-success btn-lg fw-semibold">See my results</button>
                    </form>
                </div>
            </c:when>

            <%-- 1) Immediate correction: feedback for the answer that was just given --%>
            <c:when test="${not empty feedback}">
                <c:set var="question" value="${feedbackQuestion}"/>
                <p class="fs-5 fw-semibold mb-3"><c:out value="${question.questionText}"/></p>
                <c:if test="${question.pictureResponse}">
                    <div class="text-center mb-3">
                        <img src="${fn:escapeXml(question.imageUrl)}" alt="Picture for this question"
                             class="img-fluid rounded question-image" referrerpolicy="no-referrer"
                             onerror="this.hidden = true; this.nextElementSibling.hidden = false;">
                        <div class="alert alert-warning mt-2" hidden>
                            <i class="bi bi-image"></i> The picture for this question could not be loaded.
                        </div>
                    </div>
                </c:if>
                <div class="alert ${feedback.correct ? 'alert-success' : 'alert-danger'}">
                    <h2 class="h5 alert-heading">
                        <i class="bi ${feedback.correct ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}"></i>
                        ${feedback.correct ? 'Correct!' : 'Not quite.'}
                    </h2>
                    <div>Your answer: <strong><c:out value="${empty feedback.userAnswer ? '(no answer)' : feedback.userAnswer}"/></strong></div>
                    <c:if test="${not feedback.correct}">
                        <div>Correct answer: <strong><c:out value="${feedback.correctAnswer}"/></strong></div>
                    </c:if>
                </div>
                <div class="text-end">
                    <c:choose>
                        <c:when test="${quizSession.complete}">
                            <form method="post" action="${ctx}/quiz/take">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="quizId" value="${quizSession.quizId}">
                                <input type="hidden" name="action" value="finish">
                                <button type="submit" class="btn btn-success btn-lg fw-semibold">
                                    See my results <i class="bi bi-flag-fill"></i></button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <a href="${ctx}/quiz/take" class="btn btn-light text-primary btn-lg fw-semibold">
                                Next question <i class="bi bi-arrow-right"></i></a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:when>

            <%-- 2) One question per page --%>
            <c:when test="${quizSession.multiplePages}">
                <c:set var="question" value="${quizSession.currentQuestion}"/>
                <c:choose>
                    <c:when test="${empty question}">
                        <p class="lead">You've answered every question.</p>
                        <form method="post" action="${ctx}/quiz/take">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="quizId" value="${quizSession.quizId}">
                            <input type="hidden" name="action" value="finish">
                            <button type="submit" class="btn btn-success btn-lg fw-semibold">See my results</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="text-soft mb-2">Question ${quizSession.currentNumber} of ${quizSession.totalQuestions}</p>
                        <form method="post" action="${ctx}/quiz/take" data-timer-submit>
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="quizId" value="${quizSession.quizId}">
                            <input type="hidden" name="questionId" value="${question.questionId}">
                            <c:set var="fieldName" value="answer"/>
                            <%@ include file="/WEB-INF/fragments/question-input.jspf" %>
                            <div class="text-end mt-4">
                                <button type="submit" class="btn btn-light text-primary btn-lg fw-semibold">
                                    <c:choose>
                                        <c:when test="${quizSession.immediateCorrection}">Check answer <i class="bi bi-check2"></i></c:when>
                                        <c:when test="${quizSession.currentNumber == quizSession.totalQuestions}">Finish quiz <i class="bi bi-flag-fill"></i></c:when>
                                        <c:otherwise>Next <i class="bi bi-arrow-right"></i></c:otherwise>
                                    </c:choose>
                                </button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </c:when>

            <%-- 3) All questions on one page --%>
            <c:otherwise>
                <form method="post" action="${ctx}/quiz/take" data-timer-submit>
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="quizId" value="${quizSession.quizId}">
                    <c:forEach var="question" items="${quizSession.questions}" varStatus="number">
                        <div class="glass-inner p-3 p-md-4 mb-3">
                            <p class="small text-soft mb-1">Question ${number.index + 1} of ${quizSession.totalQuestions}</p>
                            <c:set var="fieldName" value="answer_${question.questionId}"/>
                            <%@ include file="/WEB-INF/fragments/question-input.jspf" %>
                        </div>
                    </c:forEach>
                    <button type="submit" class="btn btn-success btn-lg w-100 fw-semibold">
                        <i class="bi bi-flag-fill"></i> Finish quiz</button>
                </form>
            </c:otherwise>
        </c:choose>

        <%-- What the timer sends when the page has no answer form of its own (e.g. a feedback page). --%>
        <c:if test="${not empty secondsLeft}">
            <form method="post" action="${ctx}/quiz/take" id="timeUpForm" hidden>
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="quizId" value="${quizSession.quizId}">
                <input type="hidden" name="action" value="finish">
            </form>
        </c:if>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
