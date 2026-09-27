<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Edit: ${quiz.title}"/>
<c:set var="activeNav" value="quizzes"/>
<c:set var="selectedType" value="${empty formType ? 'question_response' : formType}"/>
<%-- ${editingId} is set while a question is being edited in the form. --%>
<c:set var="editing" value="${not empty editingId}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 my-3">
        <h1 class="h3 fw-bold mb-0">
            <i class="bi bi-pencil-square"></i> Edit &ldquo;<c:out value="${quiz.title}"/>&rdquo;
        </h1>
        <a href="${ctx}/quiz?id=${quiz.quizId}" class="btn btn-success fw-semibold">
            <i class="bi bi-check2-circle"></i> Done &ndash; go to the quiz page</a>
    </div>

    <div class="row g-4">
        <%-- Form for a new question, or for the question being edited --%>
        <div class="col-lg-6">
            <section class="glass-card p-4 ${editing ? 'editing-card' : ''}" id="questionFormCard">
                <h2 class="h5 fw-semibold mb-3">
                    <c:choose>
                        <c:when test="${editing}"><i class="bi bi-pencil"></i> Edit question</c:when>
                        <c:otherwise><i class="bi bi-plus-circle"></i> Add a question</c:otherwise>
                    </c:choose>
                </h2>
                <form method="post" action="${ctx}/quiz/questions?id=${quiz.quizId}" id="questionForm" autocomplete="off">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <c:choose>
                        <c:when test="${editing}">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="questionId" value="${editingId}">
                        </c:when>
                        <c:otherwise><input type="hidden" name="action" value="add"></c:otherwise>
                    </c:choose>

                    <div class="mb-3">
                        <label for="type" class="form-label">Question type</label>
                        <select class="form-select" id="type" name="type">
                            <c:forEach var="type" items="${questionTypes}">
                                <option value="${type.dbValue}" ${type.dbValue == selectedType ? 'selected' : ''}>${type.label}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="questionText" class="form-label">Question</label>
                        <textarea class="form-control" id="questionText" name="questionText" rows="2" maxlength="500"
                                  required><c:out value="${formText}"/></textarea>
                        <div class="form-text text-soft" data-show-for="fill_blank">
                            Write ____ (underscores) where the missing word goes.</div>
                    </div>

                    <div class="mb-3" data-show-for="picture_response">
                        <label for="imageUrl" class="form-label">Image URL</label>
                        <input type="url" class="form-control" id="imageUrl" name="imageUrl" maxlength="1000"
                               placeholder="https://example.com/picture.jpg" value="${fn:escapeXml(formImageUrl)}">
                        <div class="text-center mt-2">
                            <img id="imagePreview" alt="Preview of the picture" class="img-fluid rounded question-image" hidden>
                        </div>
                        <div id="imagePreviewError" class="alert alert-warning mt-2 mb-0" hidden>
                            That image can't be loaded. Please check the URL.
                        </div>
                    </div>

                    <div class="mb-3" data-show-for="question_response fill_blank picture_response">
                        <label for="answers" class="form-label">Accepted answers <span class="small text-soft">(one per line)</span></label>
                        <textarea class="form-control" id="answers" name="answers" rows="3"
                                  placeholder="George Washington&#10;Washington"><c:out value="${formAnswers}"/></textarea>
                        <div class="form-text text-soft">Answers are checked ignoring upper/lower case and extra spaces.</div>
                    </div>

                    <div class="mb-3" data-show-for="multiple_choice">
                        <label class="form-label">Options <span class="small text-soft">(tick the correct one or ones)</span></label>
                        <%-- At least four rows; more when the question being edited has more options. --%>
                        <c:set var="choiceRows" value="${fn:length(formChoices) > 4 ? fn:length(formChoices) : 4}"/>
                        <div id="choices">
                            <c:forEach begin="0" end="${choiceRows - 1}" varStatus="row">
                                <c:set var="choice" value="${row.index < fn:length(formChoices) ? formChoices[row.index] : null}"/>
                                <div class="input-group mb-2">
                                    <div class="input-group-text">
                                        <input class="form-check-input mt-0" type="checkbox" name="correctChoice"
                                               value="${row.index}" aria-label="Option ${row.index + 1} is correct"
                                               <c:if test="${choice.correct}">checked</c:if>>
                                    </div>
                                    <input type="text" class="form-control" name="choiceText" maxlength="255"
                                           placeholder="Option ${row.index + 1}" value="${fn:escapeXml(choice.choiceText)}">
                                </div>
                            </c:forEach>
                        </div>
                        <button type="button" class="btn btn-sm btn-outline-light" id="addChoice">
                            <i class="bi bi-plus"></i> Add option</button>
                    </div>

                    <c:choose>
                        <c:when test="${editing}">
                            <div class="d-flex gap-2 mt-2">
                                <button type="submit" class="btn btn-light text-primary fw-semibold flex-grow-1">
                                    <i class="bi bi-check-lg"></i> Save changes</button>
                                <a href="${ctx}/quiz/questions?id=${quiz.quizId}" class="btn btn-outline-light">Cancel</a>
                            </div>
                            <p class="small text-soft mt-2 mb-0">Results from earlier attempts stay as they were graded.</p>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" class="btn btn-light text-primary fw-semibold w-100 mt-2">
                                <i class="bi bi-plus-circle"></i> Add question</button>
                        </c:otherwise>
                    </c:choose>
                </form>
            </section>
        </div>

        <%-- The quiz's settings, and the questions that already exist --%>
        <div class="col-lg-6">
            <section class="glass-card p-4 mb-4">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
                    <h2 class="h5 fw-semibold mb-0"><i class="bi bi-sliders"></i> Settings</h2>
                    <a href="${ctx}/quiz/edit?id=${quiz.quizId}" class="btn btn-sm btn-light text-primary fw-semibold">
                        <i class="bi bi-pencil"></i> Edit title and settings</a>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <span class="badge text-bg-light" title="${quiz.visibility.description}">
                        <i class="bi ${quiz.visibility.icon}"></i> ${quiz.visibility.label}</span>
                    <span class="badge text-bg-light">${quiz.multiplePages ? 'One question per page' : 'All questions on one page'}</span>
                    <c:if test="${quiz.randomOrder}"><span class="badge text-bg-light">Random order</span></c:if>
                    <c:if test="${quiz.immediateCorrection}"><span class="badge text-bg-light">Instant feedback</span></c:if>
                    <c:if test="${not empty quiz.timeLimitSeconds}">
                        <span class="badge text-bg-light"><i class="bi bi-stopwatch"></i> ${quiz.timeLimitMinutes} min</span>
                    </c:if>
                </div>
            </section>

            <section class="glass-card p-4 mb-4">
                <h2 class="h5 fw-semibold mb-3">Questions in this quiz (${fn:length(questions)})</h2>
                <c:if test="${empty questions}">
                    <p class="mb-0 text-soft">No questions yet. Add the first one with the form.</p>
                </c:if>
                <c:forEach var="question" items="${questions}" varStatus="number">
                    <div class="glass-inner p-3 mb-2 ${question.questionId == editingId ? 'editing-card' : ''}">
                        <div class="d-flex justify-content-between gap-2">
                            <div>
                                <span class="badge text-bg-info mb-1">${question.type.label}</span>
                                <div class="fw-semibold">${number.index + 1}. <c:out value="${question.questionText}"/></div>
                            </div>
                            <div class="d-flex align-items-start gap-2">
                                <a href="${ctx}/quiz/questions?id=${quiz.quizId}&amp;edit=${question.questionId}#questionFormCard"
                                   class="btn btn-sm btn-light text-primary" title="Edit question" aria-label="Edit question ${number.index + 1}">
                                    <i class="bi bi-pencil"></i></a>
                                <form method="post" action="${ctx}/quiz/questions?id=${quiz.quizId}" class="m-0"
                                      data-confirm="Delete this question?">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="questionId" value="${question.questionId}">
                                    <button type="submit" class="btn btn-sm btn-danger" title="Delete question"
                                            aria-label="Delete question ${number.index + 1}">
                                        <i class="bi bi-trash"></i></button>
                                </form>
                            </div>
                        </div>
                        <c:if test="${question.pictureResponse}">
                            <img src="${fn:escapeXml(question.imageUrl)}" alt="Question picture" class="img-thumbnail mt-2"
                                 style="max-height: 90px;" referrerpolicy="no-referrer"
                                 onerror="this.hidden = true; this.nextElementSibling.hidden = false;">
                            <div class="small text-warning mt-2" hidden>
                                <i class="bi bi-exclamation-triangle"></i> This picture can't be loaded &ndash; check the link.
                            </div>
                        </c:if>
                        <div class="small text-soft mt-2">
                            <c:choose>
                                <c:when test="${question.multipleChoice}">
                                    <c:forEach var="choice" items="${question.choices}">
                                        <span class="me-2">
                                            <i class="bi ${choice.correct ? 'bi-check-circle-fill text-success' : 'bi-circle'}"></i>
                                            <c:out value="${choice.choiceText}"/>
                                        </span>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    Accepted: <c:out value="${question.correctAnswerText}"/>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:forEach>
            </section>

            <section class="glass-card p-4 border-danger">
                <h2 class="h6 fw-semibold text-warning"><i class="bi bi-exclamation-octagon"></i> Danger zone</h2>
                <form method="post" action="${ctx}/quiz/questions?id=${quiz.quizId}"
                      data-confirm="Delete this quiz with all its questions and results? This can't be undone.">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="action" value="delete_quiz">
                    <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-trash"></i> Delete quiz</button>
                </form>
            </section>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
<script>
    (function () {
        var typeSelect = document.getElementById('type');
        var choices = document.getElementById('choices');
        var imageUrl = document.getElementById('imageUrl');
        var preview = document.getElementById('imagePreview');
        var previewError = document.getElementById('imagePreviewError');

        // Show only the fields that belong to the chosen question type. Hidden fields are disabled,
        // so they are neither validated nor sent with the form.
        function showFieldsForType() {
            var type = typeSelect.value;
            document.querySelectorAll('[data-show-for]').forEach(function (block) {
                var visible = block.dataset.showFor.split(' ').indexOf(type) >= 0;
                block.hidden = !visible;
                block.querySelectorAll('input, textarea').forEach(function (field) {
                    field.disabled = !visible;
                });
            });
            document.getElementById('answers').required = type !== 'multiple_choice';
            imageUrl.required = type === 'picture_response';
            choices.querySelectorAll('input[name="choiceText"]').forEach(function (field, index) {
                field.required = type === 'multiple_choice' && index < 2;
            });
        }

        // New option rows get the next index, which is what the "correct" checkbox sends.
        document.getElementById('addChoice').addEventListener('click', function () {
            var index = choices.querySelectorAll('.input-group').length;
            var row = choices.querySelector('.input-group').cloneNode(true);
            var checkbox = row.querySelector('input[type="checkbox"]');
            var text = row.querySelector('input[type="text"]');
            checkbox.value = index;
            checkbox.checked = false;
            checkbox.setAttribute('aria-label', 'Option ' + (index + 1) + ' is correct');
            text.value = '';
            text.required = false;
            text.placeholder = 'Option ' + (index + 1);
            choices.appendChild(row);
        });

        // Live preview of the picture URL.
        function showPreview() {
            var url = imageUrl.value.trim();
            previewError.hidden = true;
            if (!/^https?:\/\/.+/i.test(url)) {
                preview.hidden = true;
                return;
            }
            preview.onload = function () { preview.hidden = false; };
            preview.onerror = function () { preview.hidden = true; previewError.hidden = false; };
            preview.src = url;
        }

        typeSelect.addEventListener('change', showFieldsForType);
        imageUrl.addEventListener('change', showPreview);
        imageUrl.addEventListener('paste', function () { setTimeout(showPreview, 0); });
        showFieldsForType();
        showPreview();
    })();
</script>
</body>
</html>
