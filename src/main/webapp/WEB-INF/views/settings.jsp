<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Settings"/>
<c:set var="activeNav" value="settings"/>
<c:set var="me" value="${sessionScope.user}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <div class="mx-auto settings-panel">
        <header class="d-flex align-items-center gap-3 my-4">
            <q:avatar name="${me.displayName}" userId="${me.userId}" size="lg"/>
            <div>
                <h1 class="h3 fw-bold mb-0">Settings</h1>
                <p class="text-soft mb-0">@<c:out value="${me.username}"/> &middot;
                    <a href="${ctx}/profile" class="link-light">view your profile</a></p>
            </div>
        </header>
        <%@ include file="/WEB-INF/fragments/flash.jspf" %>

        <section class="glass-card p-4 mb-4">
            <h2 class="h5 fw-semibold mb-3"><i class="bi bi-person-badge"></i> Display name</h2>
            <form method="post" action="${ctx}/settings" class="m-0">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="action" value="name">
                <label for="displayName" class="form-label">The name other people see</label>
                <div class="input-group">
                    <input type="text" class="form-control" id="displayName" name="displayName" maxlength="50"
                           value="${fn:escapeXml(me.displayName)}">
                    <button type="submit" class="btn btn-light text-primary fw-semibold">Save</button>
                </div>
                <div class="form-text text-soft">Up to 50 characters. Leave it empty to use your username.
                    Your mantra is on <a href="${ctx}/profile" class="link-light">your profile</a>.</div>
            </form>
        </section>

        <section class="glass-card p-4 mb-4">
            <h2 class="h5 fw-semibold mb-3"><i class="bi bi-key"></i> Password</h2>
            <form method="post" action="${ctx}/settings" class="m-0" autocomplete="off">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="action" value="password">
                <div class="mb-3">
                    <label for="currentPassword" class="form-label">Current password</label>
                    <input type="password" class="form-control" id="currentPassword" name="currentPassword"
                           autocomplete="current-password" required>
                </div>
                <div class="row g-3 mb-3">
                    <div class="col-sm-6">
                        <label for="newPassword" class="form-label">New password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword"
                               minlength="8" maxlength="128" autocomplete="new-password" required>
                    </div>
                    <div class="col-sm-6">
                        <label for="confirmPassword" class="form-label">New password again</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               minlength="8" maxlength="128" autocomplete="new-password" required>
                    </div>
                </div>
                <button type="submit" class="btn btn-light text-primary fw-semibold">Change password</button>
            </form>
        </section>

        <section class="glass-card p-4 mb-4">
            <h2 class="h5 fw-semibold text-warning"><i class="bi bi-exclamation-octagon"></i> Delete account</h2>
            <p class="small">This deletes your account for good, with your quizzes, results, friends, messages
                and challenges. It can't be undone.</p>
            <form method="post" action="${ctx}/settings" class="m-0"
                  data-confirm="Really delete your account and everything in it? This can't be undone.">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="action" value="delete">
                <label for="deletePassword" class="form-label">Type your password to confirm</label>
                <div class="input-group">
                    <input type="password" class="form-control" id="deletePassword" name="password"
                           autocomplete="current-password" required>
                    <button type="submit" class="btn btn-danger fw-semibold"><i class="bi bi-trash"></i> Delete my account</button>
                </div>
            </form>
        </section>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
