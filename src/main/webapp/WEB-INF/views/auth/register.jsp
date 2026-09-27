<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Create an account"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<main class="auth-wrapper">
    <h1 class="brand-title display-6">Quizz<span class="text-warning">mosis</span></h1>

    <div class="glass-card auth-card p-4 p-md-5">
        <h2 class="h3 fw-bold mb-4 text-center">Create an account</h2>
        <%@ include file="/WEB-INF/fragments/flash.jspf" %>

        <form method="post" action="${ctx}/register" class="needs-validation" novalidate>
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" name="username"
                       value="${fn:escapeXml(username)}" pattern="[A-Za-z0-9_.\-]{3,30}" maxlength="30"
                       autocomplete="username" required autofocus>
                <div class="form-text text-soft">3-30 characters: letters, digits, '.', '_' or '-'.</div>
                <div class="invalid-feedback">Please choose a valid username.</div>
            </div>
            <div class="mb-3">
                <label for="displayName" class="form-label">Display name <span class="text-soft small">(optional)</span></label>
                <input type="text" class="form-control" id="displayName" name="displayName"
                       value="${fn:escapeXml(displayName)}" maxlength="50" autocomplete="name">
            </div>
            <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input type="password" class="form-control" id="password" name="password"
                       minlength="${minPasswordLength}" maxlength="128" autocomplete="new-password" required>
                <div class="invalid-feedback">At least ${minPasswordLength} characters, please.</div>
            </div>
            <div class="mb-4">
                <label for="confirmPassword" class="form-label">Confirm password</label>
                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                       autocomplete="new-password" required>
                <div class="invalid-feedback">The passwords don't match.</div>
            </div>
            <button type="submit" class="btn btn-light text-primary fw-semibold w-100 py-2">Sign up</button>
        </form>

        <p class="text-center small mt-4 mb-0">
            Already have an account? <a href="${ctx}/login" class="link-light fw-semibold">Log in</a>
        </p>
    </div>
</main>
<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
