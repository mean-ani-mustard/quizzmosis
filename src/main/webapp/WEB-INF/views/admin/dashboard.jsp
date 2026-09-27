<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Admin"/>
<c:set var="activeNav" value="admin"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <header class="text-center py-4">
        <h1 class="display-6 fw-bold"><i class="bi bi-shield-fill me-2"></i>Administration</h1>
        <p class="lead mb-0">Manage users, quizzes and announcements.</p>
    </header>
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <section class="row g-3 mb-4">
        <div class="col-4"><div class="stat-card"><div class="stat-number">${userCount}</div><div class="stat-label">Users</div></div></div>
        <div class="col-4"><div class="stat-card"><div class="stat-number">${quizCount}</div><div class="stat-label">Quizzes</div></div></div>
        <div class="col-4"><div class="stat-card"><div class="stat-number">${attemptCount}</div><div class="stat-label">Quizzes taken</div></div></div>
    </section>

    <%-- Announcements --%>
    <section class="glass-card p-4 mb-4">
        <h2 class="h4 fw-semibold mb-3"><i class="bi bi-megaphone-fill me-2"></i>Announcements</h2>
        <form method="post" action="${ctx}/admin" class="row g-2 mb-4">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="action" value="create_announcement">
            <div class="col-md-4">
                <input type="text" class="form-control" name="title" maxlength="150" placeholder="Title" required>
            </div>
            <div class="col-md-6">
                <textarea class="form-control" name="content" rows="1" maxlength="2000" placeholder="Text" required></textarea>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-success w-100"><i class="bi bi-plus-circle"></i> Publish</button>
            </div>
        </form>
        <div class="table-responsive">
            <table class="table table-glass align-middle">
                <thead><tr><th>Title</th><th>Text</th><th>Status</th><th>Created</th><th class="text-end">Actions</th></tr></thead>
                <tbody>
                <c:forEach var="announcement" items="${announcements}">
                    <tr>
                        <td class="fw-semibold"><c:out value="${announcement.title}"/></td>
                        <td class="small pre-wrap"><c:out value="${announcement.content}"/></td>
                        <td><span class="badge ${announcement.active ? 'bg-success' : 'bg-secondary'}">${announcement.active ? 'Visible' : 'Hidden'}</span></td>
                        <td class="small text-nowrap"><fmt:formatDate value="${announcement.createdAt}" pattern="MMM d, yyyy"/></td>
                        <td class="text-end text-nowrap">
                            <form method="post" action="${ctx}/admin" class="d-inline">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${announcement.announcementId}">
                                <button type="submit" name="action" value="toggle_announcement" class="btn btn-sm btn-outline-light">
                                    ${announcement.active ? 'Hide' : 'Show'}</button>
                            </form>
                            <form method="post" action="${ctx}/admin" class="d-inline" data-confirm="Delete this announcement?">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${announcement.announcementId}">
                                <button type="submit" name="action" value="delete_announcement" class="btn btn-sm btn-danger"><i class="bi bi-trash"></i></button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>

    <%-- Users --%>
    <section class="glass-card p-4 mb-4">
        <h2 class="h4 fw-semibold mb-3"><i class="bi bi-people-fill me-2"></i>Users</h2>
        <div class="table-responsive">
            <table class="table table-glass align-middle">
                <thead><tr><th>Username</th><th>Display name</th><th>Role</th><th>Joined</th><th>Last login</th><th class="text-end">Actions</th></tr></thead>
                <tbody>
                <c:forEach var="account" items="${users}">
                    <c:set var="isMe" value="${account.userId == sessionScope.user.userId}"/>
                    <tr>
                        <td><a href="${ctx}/profile?id=${account.userId}" class="link-light"><c:out value="${account.username}"/></a></td>
                        <td><c:out value="${account.displayName}"/></td>
                        <td><span class="badge ${account.admin ? 'bg-danger' : 'bg-primary'}">${account.admin ? 'Admin' : 'User'}</span></td>
                        <td class="small text-nowrap"><fmt:formatDate value="${account.createdAt}" pattern="MMM d, yyyy"/></td>
                        <td class="small text-nowrap">
                            <c:choose>
                                <c:when test="${empty account.lastLogin}"><span class="text-soft">never</span></c:when>
                                <c:otherwise><fmt:formatDate value="${account.lastLogin}" pattern="MMM d, HH:mm"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end text-nowrap">
                            <c:choose>
                                <c:when test="${isMe}"><span class="small text-soft">that's you</span></c:when>
                                <c:otherwise>
                                    <form method="post" action="${ctx}/admin" class="d-inline">
                                        <input type="hidden" name="_csrf" value="${csrfToken}">
                                        <input type="hidden" name="id" value="${account.userId}">
                                        <button type="submit" name="action" value="${account.admin ? 'demote' : 'promote'}"
                                                class="btn btn-sm ${account.admin ? 'btn-outline-light' : 'btn-warning'}">
                                            ${account.admin ? 'Remove admin' : 'Make admin'}</button>
                                    </form>
                                    <form method="post" action="${ctx}/admin" class="d-inline"
                                          data-confirm="Delete ${fn:escapeXml(account.username)} and everything they created? This can't be undone.">
                                        <input type="hidden" name="_csrf" value="${csrfToken}">
                                        <input type="hidden" name="id" value="${account.userId}">
                                        <button type="submit" name="action" value="delete_user" class="btn btn-sm btn-danger"><i class="bi bi-trash"></i></button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>

    <%-- Quizzes --%>
    <section class="glass-card p-4 mb-4">
        <h2 class="h4 fw-semibold mb-3"><i class="bi bi-list-check me-2"></i>Quizzes</h2>
        <div class="table-responsive">
            <table class="table table-glass align-middle">
                <thead><tr><th>Title</th><th>Author</th><th>Visible to</th><th>Questions</th><th>Plays</th><th>Created</th><th class="text-end">Actions</th></tr></thead>
                <tbody>
                <c:forEach var="quiz" items="${quizzes}">
                    <tr>
                        <td><a href="${ctx}/quiz?id=${quiz.quizId}" class="link-light fw-semibold"><c:out value="${quiz.title}"/></a></td>
                        <td><c:out value="${quiz.ownerUsername}"/></td>
                        <td class="small text-nowrap"><i class="bi ${quiz.visibility.icon}"></i> ${quiz.visibility.label}</td>
                        <td>${quiz.questionCount}</td>
                        <td>${quiz.attemptCount}</td>
                        <td class="small text-nowrap"><fmt:formatDate value="${quiz.creationDate}" pattern="MMM d, yyyy"/></td>
                        <td class="text-end text-nowrap">
                            <form method="post" action="${ctx}/admin" class="d-inline"
                                  data-confirm="Remove every result of this quiz?">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${quiz.quizId}">
                                <button type="submit" name="action" value="clear_history" class="btn btn-sm btn-outline-light">Clear history</button>
                            </form>
                            <form method="post" action="${ctx}/admin" class="d-inline"
                                  data-confirm="Delete this quiz with all its questions and results?">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${quiz.quizId}">
                                <button type="submit" name="action" value="delete_quiz" class="btn btn-sm btn-danger"><i class="bi bi-trash"></i></button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
