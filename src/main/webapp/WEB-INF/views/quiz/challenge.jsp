<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Challenge a friend"/>
<c:set var="activeNav" value="quizzes"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-5">
    <div class="glass-card p-4 p-md-5 mx-auto" style="max-width: 560px;">
        <div class="text-center mb-4">
            <i class="bi bi-flag-fill text-warning display-6"></i>
            <h1 class="h3 fw-bold mt-2 mb-1">Challenge a friend</h1>
            <p class="mb-0">to beat you on <a href="${ctx}/quiz?id=${quiz.quizId}" class="link-light fw-semibold"><c:out value="${quiz.title}"/></a></p>
        </div>
        <%@ include file="/WEB-INF/fragments/flash.jspf" %>

        <c:choose>
            <c:when test="${not hasFriends}">
                <p class="text-center">You need friends before you can challenge them.</p>
                <div class="text-center">
                    <a href="${ctx}/friends" class="btn btn-light text-primary fw-semibold"><i class="bi bi-people-fill"></i> Find friends</a>
                </div>
            </c:when>
            <c:when test="${empty friends}">
                <p class="text-center mb-0">None of your friends can see this quiz, so there is nobody to challenge.</p>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/quiz/challenge?id=${quiz.quizId}">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <div class="mb-3">
                        <label for="friendId" class="form-label">Friend</label>
                        <select name="friendId" id="friendId" class="form-select" required>
                            <option value="">Choose a friend...</option>
                            <c:forEach var="friend" items="${friends}">
                                <option value="${friend.userId}"><c:out value="${friend.displayName} (@${friend.username})"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-4">
                        <label for="message" class="form-label">Message <span class="small text-soft">(optional)</span></label>
                        <textarea name="message" id="message" class="form-control" rows="2" maxlength="500"
                                  placeholder="Can you beat my score?"></textarea>
                    </div>
                    <button type="submit" class="btn btn-warning w-100 fw-semibold"><i class="bi bi-send"></i> Send challenge</button>
                    <p class="small text-soft text-center mt-3 mb-0">
                        Your friend can accept or decline. Either way you get a message, and their score once they finish.</p>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
