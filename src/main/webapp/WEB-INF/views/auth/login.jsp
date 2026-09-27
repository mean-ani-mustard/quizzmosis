<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Log in"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<main class="auth-wrapper">
    <h1 class="brand-title display-6">Quizz<span class="text-warning">mosis</span></h1>

    <div class="glass-card auth-card p-4 p-md-5">
        <h2 class="h3 fw-bold mb-4 text-center">Log in</h2>

        <c:if test="${param.loggedOut != null}">
            <div class="alert alert-info">You have been logged out. See you soon!</div>
        </c:if>
        <%@ include file="/WEB-INF/fragments/flash.jspf" %>

        <form method="post" action="${ctx}/login" class="needs-validation" novalidate>
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" name="username"
                       value="${fn:escapeXml(username)}" autocomplete="username" required autofocus>
                <div class="invalid-feedback">Please enter your username.</div>
            </div>
            <div class="mb-4">
                <label for="password" class="form-label">Password</label>
                <input type="password" class="form-control" id="password" name="password"
                       autocomplete="current-password" required>
                <div class="invalid-feedback">Please enter your password.</div>
            </div>
            <button type="submit" class="btn btn-light text-primary fw-semibold w-100 py-2">Log in</button>
        </form>

        <p class="text-center small mt-4 mb-0">
            New here? <a href="${ctx}/register" class="link-light fw-semibold">Create an account</a>
        </p>
    </div>

    <p class="small text-soft mt-3 text-center">
        Trying the demo? Log in as <strong>alice</strong> (admin), <strong>bob</strong> or <strong>carol</strong>
        with the password <code class="text-warning">password123</code>.
    </p>
</main>
<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
