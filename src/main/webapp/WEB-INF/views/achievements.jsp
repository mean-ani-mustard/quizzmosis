<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Achievements"/>
<c:set var="activeNav" value="achievements"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <header class="text-center py-4">
        <i class="bi bi-patch-check-fill achievement-icon display-4"></i>
        <h1 class="display-6 fw-bold mt-2">Your achievements</h1>
        <p class="lead mb-0">${earnedCount} of ${fn:length(achievements)} earned &ndash; keep playing and creating quizzes!</p>
    </header>

    <div class="row g-4 justify-content-center">
        <c:forEach var="achievement" items="${achievements}">
            <div class="col-6 col-md-4 col-lg-3">
                <div class="achievement-card ${achievement.earned ? '' : 'locked'}">
                    <i class="bi ${achievement.icon} achievement-icon"></i>
                    <h2 class="h6 fw-semibold mt-2 mb-1"><c:out value="${achievement.name}"/></h2>
                    <p class="small mb-1"><c:out value="${achievement.description}"/></p>
                    <c:choose>
                        <c:when test="${achievement.earned}">
                            <span class="small text-warning">Earned <fmt:formatDate value="${achievement.earnedAt}" pattern="MMM d, yyyy"/></span>
                        </c:when>
                        <c:otherwise>
                            <span class="small"><i class="bi bi-lock-fill"></i> Locked</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:forEach>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
<c:if test="${earnedCount > 0}">
    <script>
        // A few fireworks to celebrate. (Plain string concatenation instead of JavaScript template
        // literals: inside a JSP, "$" + "{...}" is evaluated as EL on the server before the browser sees it.)
        (function () {
            var colors = ['#fff176', '#ffd700', '#ff4081', '#40c4ff', '#7c4dff', '#00e676'];
            function launch() {
                var x = window.innerWidth * (0.1 + Math.random() * 0.8);
                var y = window.innerHeight * (0.15 + Math.random() * 0.3);
                for (var i = 0; i < 18; i++) {
                    var angle = (i / 18) * 2 * Math.PI;
                    var dot = document.createElement('div');
                    dot.className = 'firework';
                    dot.style.left = x + 'px';
                    dot.style.top = y + 'px';
                    dot.style.background = colors[Math.floor(Math.random() * colors.length)];
                    dot.style.setProperty('--dx', Math.round(Math.cos(angle) * 110) + 'px');
                    dot.style.setProperty('--dy', Math.round(Math.sin(angle) * 110) + 'px');
                    document.body.appendChild(dot);
                    setTimeout(dot.remove.bind(dot), 1300);
                }
            }
            for (var n = 0; n < 5; n++) {
                setTimeout(launch, 300 + n * 450);
            }
        })();
    </script>
</c:if>
</body>
</html>
