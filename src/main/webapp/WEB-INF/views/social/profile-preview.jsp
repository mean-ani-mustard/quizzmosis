<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="q" tagdir="/WEB-INF/tags" %>
<%--
    Not a whole page: the inside of the profile preview window (fragments/profile-preview-modal.jspf),
    loaded by app.js. Links with data-preview-url open another preview in the same window.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<div class="text-center">
    <q:avatar name="${profileUser.displayName}" userId="${profileUser.userId}" size="lg"/>
    <h2 class="h4 fw-bold mt-2 mb-0">
        <c:out value="${profileUser.displayName}"/>
        <c:if test="${profileUser.admin}"><span class="badge bg-danger fs-6 align-middle">Admin</span></c:if>
    </h2>
    <p class="text-soft small mb-2">@<c:out value="${profileUser.username}"/> &middot; member since
        <fmt:formatDate value="${profileUser.createdAt}" pattern="MMMM yyyy"/></p>
    <c:if test="${not empty profileUser.mantra}">
        <p class="mantra mb-3">&ldquo;<c:out value="${profileUser.mantra}"/>&rdquo;</p>
    </c:if>
    <div class="d-flex justify-content-center flex-wrap gap-3 small mb-3">
        <span><i class="bi bi-pencil-square"></i> ${quizCount} ${quizCount == 1 ? 'quiz' : 'quizzes'} created</span>
        <span><i class="bi bi-check2-square"></i> ${attemptCount} taken</span>
        <span><i class="bi bi-people"></i> ${friendCount} ${friendCount == 1 ? 'friend' : 'friends'}</span>
    </div>
</div>

<c:if test="${not empty achievements}">
    <div class="d-flex justify-content-center flex-wrap gap-2 mb-3">
        <c:forEach var="achievement" items="${achievements}">
            <span class="badge rounded-pill text-bg-light" title="${fn:escapeXml(achievement.description)}">
                <i class="bi ${achievement.icon} text-warning"></i> <c:out value="${achievement.name}"/></span>
        </c:forEach>
    </div>
</c:if>

<c:if test="${status != 'SELF'}">
    <div class="glass-inner p-3 mb-3">
        <h3 class="h6 fw-semibold mb-2"><i class="bi bi-people-fill"></i> Mutual friends (${fn:length(mutualFriends)})</h3>
        <c:choose>
            <c:when test="${empty mutualFriends}">
                <p class="small text-soft mb-0">You don't have any friends in common yet.</p>
            </c:when>
            <c:otherwise>
                <div class="d-flex flex-wrap gap-2">
                    <c:forEach var="friend" items="${mutualFriends}">
                        <a href="${ctx}/profile?id=${friend.userId}" class="badge rounded-pill text-bg-light text-decoration-none"
                           data-preview-url="${ctx}/profile/preview?id=${friend.userId}">
                            <q:avatar name="${friend.displayName}" userId="${friend.userId}"/> <c:out value="${friend.displayName}"/></a>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<div class="d-flex justify-content-center flex-wrap gap-2">
    <a href="${ctx}/profile?id=${profileUser.userId}" class="btn btn-sm btn-outline-light"><i class="bi bi-person-lines-fill"></i> Full profile</a>
    <c:set var="buttonUser" value="${profileUser}"/>
    <c:set var="buttonStatus" value="${status}"/>
    <%@ include file="/WEB-INF/fragments/friend-button.jspf" %>
</div>
