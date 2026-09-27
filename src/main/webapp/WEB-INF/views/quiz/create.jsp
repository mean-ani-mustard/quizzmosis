<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Used both for creating a quiz and, with ${editing}, for changing an existing one. --%>
<c:set var="pageTitle" value="${editing ? 'Edit quiz' : 'Create a quiz'}"/>
<c:set var="activeNav" value="${editing ? 'quizzes' : 'create'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-5">
    <div class="glass-card p-4 p-md-5 mx-auto" style="max-width: 640px;">
        <h1 class="h3 fw-bold mb-4">
            <c:choose>
                <c:when test="${editing}"><i class="bi bi-sliders"></i> Edit &ldquo;<c:out value="${quiz.title}"/>&rdquo;</c:when>
                <c:otherwise><i class="bi bi-plus-circle"></i> Create a new quiz</c:otherwise>
            </c:choose>
        </h1>
        <%@ include file="/WEB-INF/fragments/flash.jspf" %>

        <c:url var="formAction" value="${editing ? '/quiz/edit' : '/quiz/create'}">
            <c:if test="${editing}"><c:param name="id" value="${quiz.quizId}"/></c:if>
        </c:url>
        <form method="post" action="${formAction}" autocomplete="off">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <div class="mb-3">
                <label for="title" class="form-label">Title</label>
                <input type="text" class="form-control" id="title" name="title" maxlength="100"
                       value="${fn:escapeXml(quiz.title)}" required>
            </div>
            <div class="mb-4">
                <label for="description" class="form-label">Description <span class="small text-soft">(optional)</span></label>
                <textarea class="form-control" id="description" name="description" rows="3"
                          maxlength="500"><c:out value="${quiz.description}"/></textarea>
            </div>

            <fieldset class="glass-inner p-3 mb-4">
                <legend class="h6 fw-semibold">Settings</legend>
                <div class="form-check mb-2">
                    <input class="form-check-input" type="checkbox" id="randomOrder" name="randomOrder"
                           <c:if test="${quiz.randomOrder}">checked</c:if>>
                    <label class="form-check-label" for="randomOrder">Show the questions in random order</label>
                </div>
                <div class="form-check mb-2">
                    <input class="form-check-input" type="checkbox" id="multiplePages" name="multiplePages"
                           <c:if test="${quiz.multiplePages}">checked</c:if>>
                    <label class="form-check-label" for="multiplePages">One question per page</label>
                </div>
                <div class="form-check mb-3">
                    <input class="form-check-input" type="checkbox" id="immediateCorrection" name="immediateCorrection"
                           <c:if test="${quiz.immediateCorrection}">checked</c:if>>
                    <label class="form-check-label" for="immediateCorrection">
                        Show whether each answer was right straight away
                        <span class="small text-soft">(needs "one question per page")</span>
                    </label>
                </div>
                <label for="timeLimitMinutes" class="form-label mb-1">
                    <i class="bi bi-stopwatch"></i> Time limit <span class="small text-soft">(optional)</span></label>
                <div class="input-group time-limit-input">
                    <input type="number" class="form-control" id="timeLimitMinutes" name="timeLimitMinutes"
                           min="1" max="180" step="1" placeholder="No limit" value="${quiz.timeLimitMinutes}">
                    <span class="input-group-text">minutes</span>
                </div>
                <div class="form-text text-soft">When time runs out, the quiz ends and unanswered questions count as wrong.</div>
            </fieldset>

            <fieldset class="glass-inner p-3 mb-4">
                <legend class="h6 fw-semibold">Who can see this quiz?</legend>
                <c:forEach var="option" items="${visibilities}">
                    <div class="form-check mb-2">
                        <%-- New quizzes start as public; after a validation error the choice is kept. --%>
                        <input class="form-check-input" type="radio" name="visibility" id="visibility_${option.dbValue}"
                               value="${option.dbValue}"
                               <c:if test="${(empty quiz and option == 'PUBLIC') or quiz.visibility == option}">checked</c:if>>
                        <label class="form-check-label" for="visibility_${option.dbValue}">
                            <i class="bi ${option.icon}"></i> ${option.label}
                            <span class="small text-soft">&ndash; ${option.description}</span>
                        </label>
                    </div>
                </c:forEach>
                <p class="small text-soft mb-0">You can change all of this later.</p>
            </fieldset>

            <c:choose>
                <c:when test="${editing}">
                    <div class="d-flex flex-wrap gap-2">
                        <button type="submit" class="btn btn-light text-primary fw-semibold flex-grow-1">
                            <i class="bi bi-check-lg"></i> Save changes</button>
                        <a href="${ctx}/quiz/questions?id=${quiz.quizId}" class="btn btn-outline-light">Cancel</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="small text-soft"><i class="bi bi-info-circle"></i> You'll add the questions on the next page.</p>
                    <button type="submit" class="btn btn-light text-primary fw-semibold w-100">
                        <i class="bi bi-arrow-right-circle"></i> Create quiz and add questions
                    </button>
                </c:otherwise>
            </c:choose>
        </form>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
<script>
    // Immediate correction only makes sense with one question per page.
    (function () {
        var multiplePages = document.getElementById('multiplePages');
        var immediate = document.getElementById('immediateCorrection');
        function sync() {
            immediate.disabled = !multiplePages.checked;
            if (!multiplePages.checked) {
                immediate.checked = false;
            }
        }
        multiplePages.addEventListener('change', sync);
        sync();
    })();
</script>
</body>
</html>
