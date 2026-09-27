<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Friends"/>
<c:set var="activeNav" value="friends"/>
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
        <h1 class="display-6 fw-bold"><i class="bi bi-people-fill me-2"></i>Friends</h1>
        <p class="lead mb-0">Find people, answer requests and challenge your friends.</p>
    </header>

    <div class="row g-4">
        <div class="col-lg-7">
            <section class="glass-card p-4 mb-4">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-search"></i> Find people</h2>
                <form method="get" action="${ctx}/friends" class="d-flex gap-2" role="search">
                    <input type="search" name="q" class="form-control" placeholder="Username or display name"
                           value="${fn:escapeXml(query)}" aria-label="Search users">
                    <button type="submit" class="btn btn-light text-primary">Search</button>
                </form>
                <c:if test="${not empty query}">
                    <div class="mt-3">
                        <c:if test="${empty results}">
                            <p class="mb-0 text-soft">Nobody matches &ldquo;<c:out value="${query}"/>&rdquo;.</p>
                        </c:if>
                        <c:forEach var="person" items="${results}">
                            <%@ include file="/WEB-INF/fragments/person-row.jspf" %>
                        </c:forEach>
                    </div>
                </c:if>
            </section>

            <c:if test="${not empty suggestions}">
                <section class="glass-card p-4 mb-4">
                    <h2 class="h5 fw-semibold mb-1"><i class="bi bi-stars text-warning"></i> Suggested for you</h2>
                    <p class="small text-soft mb-3">Friends of your friends. Preview them to see who you have in common.</p>
                    <c:forEach var="person" items="${suggestions}">
                        <%@ include file="/WEB-INF/fragments/person-row.jspf" %>
                    </c:forEach>
                </section>
            </c:if>

            <section class="glass-card p-4" id="people">
                <h2 class="h5 fw-semibold mb-1"><i class="bi bi-compass"></i> Browse people</h2>
                <p class="small text-soft mb-3">Everyone you aren't friends with yet. The most friends in common come first.</p>
                <c:if test="${empty people.items}">
                    <p class="mb-0 text-soft">Nobody else to meet right now.</p>
                </c:if>
                <c:forEach var="person" items="${people.items}">
                    <%@ include file="/WEB-INF/fragments/person-row.jspf" %>
                </c:forEach>
                <c:set var="page" value="${people}"/>
                <c:url var="pageLink" value="/friends">
                    <c:if test="${not empty query}"><c:param name="q" value="${query}"/></c:if>
                </c:url>
                <c:set var="pageAnchor" value="#people"/>
                <%@ include file="/WEB-INF/fragments/pagination.jspf" %>
            </section>
        </div>

        <div class="col-lg-5">
            <section class="glass-card p-4 mb-4">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-person-plus-fill"></i> Friend requests for you</h2>
                <c:if test="${empty incoming}"><p class="mb-0 text-soft">No open requests.</p></c:if>
                <c:forEach var="person" items="${incoming}">
                    <div class="glass-inner p-2 px-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <a href="${ctx}/profile?id=${person.userId}" class="link-light person-link"
                           data-preview-url="${ctx}/profile/preview?id=${person.userId}">
                            <q:avatar name="${person.displayName}" userId="${person.userId}"/> <c:out value="${person.displayName}"/>
                        </a>
                        <form method="post" action="${ctx}/friends" class="m-0 d-flex gap-2">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${person.userId}">
                            <button type="submit" name="action" value="accept" class="btn btn-sm btn-success"><i class="bi bi-check"></i> Accept</button>
                            <button type="submit" name="action" value="reject" class="btn btn-sm btn-outline-light"><i class="bi bi-x"></i> Decline</button>
                        </form>
                    </div>
                </c:forEach>

                <h2 class="h6 fw-semibold mt-4 mb-3">Requests you sent</h2>
                <c:if test="${empty outgoing}"><p class="mb-0 text-soft">None.</p></c:if>
                <c:forEach var="person" items="${outgoing}">
                    <div class="glass-inner p-2 px-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <a href="${ctx}/profile?id=${person.userId}" class="link-light person-link"
                           data-preview-url="${ctx}/profile/preview?id=${person.userId}">
                            <q:avatar name="${person.displayName}" userId="${person.userId}"/> <c:out value="${person.displayName}"/>
                        </a>
                        <form method="post" action="${ctx}/friends" class="m-0">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${person.userId}">
                            <button type="submit" name="action" value="cancel" class="btn btn-sm btn-outline-light">Cancel</button>
                        </form>
                    </div>
                </c:forEach>
            </section>

            <section class="glass-card p-4">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-heart-fill"></i> Your friends (${fn:length(friends)})</h2>
                <c:if test="${empty friends}"><p class="mb-0 text-soft">No friends yet &ndash; the people on the left are a good start.</p></c:if>
                <c:forEach var="friend" items="${friends}">
                    <div class="glass-inner p-2 px-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <div>
                            <a href="${ctx}/profile?id=${friend.userId}" class="link-light person-link"><q:avatar name="${friend.displayName}" userId="${friend.userId}"/> <c:out value="${friend.displayName}"/></a>
                            <span class="small text-soft">@<c:out value="${friend.username}"/></span>
                        </div>
                        <div class="d-flex gap-2">
                            <a href="${ctx}/messages?tab=notes&amp;to=${friend.userId}" class="btn btn-sm btn-outline-light"
                               aria-label="Send ${fn:escapeXml(friend.displayName)} a note"><i class="bi bi-chat-left-text"></i></a>
                            <form method="post" action="${ctx}/friends" class="m-0"
                                  data-confirm="Remove ${fn:escapeXml(friend.displayName)} from your friends?">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="userId" value="${friend.userId}">
                                <button type="submit" name="action" value="remove" class="btn btn-sm btn-outline-light">Unfriend</button>
                            </form>
                        </div>
                    </div>
                </c:forEach>
            </section>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/profile-preview-modal.jspf" %>
<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
