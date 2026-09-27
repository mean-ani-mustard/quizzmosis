<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${profileUser.displayName}"/>
<c:set var="activeNav" value="${status == 'SELF' ? 'profile' : 'friends'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/fragments/head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/fragments/navbar.jspf" %>

<main class="container py-4">
    <%@ include file="/WEB-INF/fragments/flash.jspf" %>

    <header class="glass-card p-4 mb-4">
        <div class="d-flex flex-wrap align-items-center gap-4">
            <q:avatar name="${profileUser.displayName}" userId="${profileUser.userId}" size="xl"/>
            <div class="flex-grow-1">
                <h1 class="h2 fw-bold mb-0">
                    <c:out value="${profileUser.displayName}"/>
                    <c:if test="${profileUser.admin}"><span class="badge bg-danger fs-6 align-middle">Admin</span></c:if>
                </h1>
                <p class="text-soft mb-2">@<c:out value="${profileUser.username}"/> &middot; member since
                    <fmt:formatDate value="${profileUser.createdAt}" pattern="MMMM yyyy"/></p>

                <%-- The mantra: a short line people write about themselves. You can edit your own here. --%>
                <c:choose>
                    <c:when test="${status == 'SELF'}">
                        <c:if test="${not empty profileUser.mantra}">
                            <div class="d-flex align-items-center flex-wrap gap-2 mb-2">
                                <p class="mantra mb-0">&ldquo;<c:out value="${profileUser.mantra}"/>&rdquo;</p>
                                <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="collapse"
                                        data-bs-target="#mantraForm" aria-expanded="false" aria-controls="mantraForm">
                                    <i class="bi bi-pencil"></i> Edit</button>
                            </div>
                        </c:if>
                        <form id="mantraForm" method="post" action="${ctx}/profile" class="mb-3 mantra-form ${empty profileUser.mantra ? '' : 'collapse'}">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="action" value="mantra">
                            <label for="mantra" class="form-label small text-soft mb-1">
                                ${empty profileUser.mantra ? 'Add a mantra: one line that sums you up.' : 'Your mantra'}</label>
                            <div class="input-group">
                                <input type="text" id="mantra" name="mantra" class="form-control" maxlength="150"
                                       value="${fn:escapeXml(profileUser.mantra)}" placeholder="e.g. Curiosity first, answers second.">
                                <button type="submit" class="btn btn-light text-primary">Save</button>
                            </div>
                            <div class="form-text text-soft">Up to 150 characters. Save it empty to remove it.</div>
                        </form>
                    </c:when>
                    <c:when test="${not empty profileUser.mantra}">
                        <p class="mantra mb-2">&ldquo;<c:out value="${profileUser.mantra}"/>&rdquo;</p>
                    </c:when>
                </c:choose>

                <div class="d-flex flex-wrap gap-3 small">
                    <span><i class="bi bi-pencil-square"></i> ${fn:length(quizzes)} ${fn:length(quizzes) == 1 ? 'quiz' : 'quizzes'} created</span>
                    <span><i class="bi bi-check2-square"></i> ${attemptCount} ${attemptCount == 1 ? 'quiz' : 'quizzes'} taken</span>
                    <span><i class="bi bi-people"></i> ${friendCount} ${friendCount == 1 ? 'friend' : 'friends'}</span>
                </div>
            </div>

            <%-- Friendship buttons depend on how you are related to this person. --%>
            <div class="d-flex flex-wrap gap-2">
                <c:choose>
                    <c:when test="${status == 'SELF'}">
                        <span class="badge text-bg-light fs-6">This is you</span>
                    </c:when>
                    <c:when test="${status == 'FRIENDS'}">
                        <a href="${ctx}/messages?tab=notes&amp;to=${profileUser.userId}" class="btn btn-light text-primary">
                            <i class="bi bi-chat-left-text"></i> Send a note</a>
                        <form method="post" action="${ctx}/friends" class="m-0"
                              data-confirm="Remove ${fn:escapeXml(profileUser.displayName)} from your friends?">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${profileUser.userId}">
                            <input type="hidden" name="returnTo" value="profile">
                            <button type="submit" name="action" value="remove" class="btn btn-outline-light">Unfriend</button>
                        </form>
                    </c:when>
                    <c:when test="${status == 'REQUEST_SENT'}">
                        <form method="post" action="${ctx}/friends" class="m-0">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${profileUser.userId}">
                            <input type="hidden" name="returnTo" value="profile">
                            <span class="badge bg-warning text-dark fs-6 me-2"><i class="bi bi-hourglass-split"></i> Request sent</span>
                            <button type="submit" name="action" value="cancel" class="btn btn-sm btn-outline-light">Cancel</button>
                        </form>
                    </c:when>
                    <c:when test="${status == 'REQUEST_RECEIVED'}">
                        <form method="post" action="${ctx}/friends" class="m-0 d-flex gap-2">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${profileUser.userId}">
                            <input type="hidden" name="returnTo" value="profile">
                            <button type="submit" name="action" value="accept" class="btn btn-success"><i class="bi bi-check"></i> Accept request</button>
                            <button type="submit" name="action" value="reject" class="btn btn-outline-light">Decline</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/friends" class="m-0">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="userId" value="${profileUser.userId}">
                            <input type="hidden" name="returnTo" value="profile">
                            <button type="submit" name="action" value="send" class="btn btn-success"><i class="bi bi-person-plus"></i> Add friend</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </header>

    <div class="row g-4">
        <%-- Mutual friends help to decide whether to add someone (hidden for friends with none). --%>
        <c:if test="${mutualFriends != null and (not empty mutualFriends or status != 'FRIENDS')}">
            <div class="col-12">
                <section class="glass-card p-4">
                    <h2 class="h5 fw-semibold mb-3"><i class="bi bi-people-fill"></i> Mutual friends (${fn:length(mutualFriends)})</h2>
                    <c:choose>
                        <c:when test="${empty mutualFriends}">
                            <p class="mb-0 text-soft">You and <c:out value="${profileUser.displayName}"/> don't have any friends in common yet.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="d-flex flex-wrap gap-2">
                                <c:forEach var="friend" items="${mutualFriends}">
                                    <a href="${ctx}/profile?id=${friend.userId}" class="glass-inner px-3 py-2 link-light text-decoration-none">
                                        <q:avatar name="${friend.displayName}" userId="${friend.userId}"/> <c:out value="${friend.displayName}"/></a>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </c:if>
        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-award-fill text-warning"></i> Achievements</h2>
                <c:if test="${empty achievements}"><p class="mb-0 text-soft">No achievements yet.</p></c:if>
                <div class="d-flex flex-wrap gap-3">
                    <c:forEach var="achievement" items="${achievements}">
                        <span class="glass-inner px-3 py-2" data-bs-toggle="tooltip" data-bs-title="${fn:escapeXml(achievement.description)}">
                            <i class="bi ${achievement.icon} text-warning"></i> <c:out value="${achievement.name}"/>
                        </span>
                    </c:forEach>
                </div>
            </section>
        </div>
        <div class="col-lg-6">
            <section class="glass-card p-4 h-100">
                <h2 class="h5 fw-semibold mb-3"><i class="bi bi-lightbulb-fill text-warning"></i> Quizzes created</h2>
                <c:if test="${empty quizzes}"><p class="mb-0 text-soft">No quizzes yet.</p></c:if>
                <ul class="list-unstyled mb-0 scroll-box">
                    <c:forEach var="quiz" items="${quizzes}">
                        <li class="mb-2">
                            <a href="${ctx}/quiz?id=${quiz.quizId}" class="link-light fw-semibold"><c:out value="${quiz.title}"/></a>
                            <%@ include file="/WEB-INF/fragments/visibility-badge.jspf" %>
                            <span class="small text-soft">&middot; ${quiz.questionCount} ${quiz.questionCount == 1 ? 'question' : 'questions'} &middot; ${quiz.attemptCount} ${quiz.attemptCount == 1 ? 'play' : 'plays'}</span>
                        </li>
                    </c:forEach>
                </ul>
            </section>
        </div>
        <%-- Recent results and the challenge history sit side by side on wide screens. --%>
        <c:if test="${recentAttempts != null}">
            <div class="${challengeHistory != null ? 'col-lg-6' : 'col-12'}">
                <section class="glass-card p-4 h-100">
                    <h2 class="h5 fw-semibold mb-3"><i class="bi bi-clock-history"></i> Recent results</h2>
                    <c:choose>
                        <c:when test="${empty recentAttempts}"><p class="mb-0 text-soft">No quizzes taken yet.</p></c:when>
                        <c:otherwise>
                            <c:forEach var="attempt" items="${recentAttempts}">
                                <div class="glass-inner p-2 px-3 mb-2 d-flex justify-content-between flex-wrap gap-2">
                                    <a href="${ctx}/quiz?id=${attempt.quizId}" class="link-light"><c:out value="${attempt.quizTitle}"/></a>
                                    <span>
                                        <strong>${attempt.score}/${attempt.totalQuestions}</strong> (${attempt.percentage}%)
                                        <span class="small text-soft ms-2"><fmt:formatDate value="${attempt.attemptDate}" pattern="MMM d, yyyy"/></span>
                                    </span>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </c:if>

        <%-- Challenges are never deleted. Friends see this list unless the user hid it. --%>
        <c:if test="${challengeHistory != null}">
            <div class="col-lg-6">
                <section id="challenge-history" class="glass-card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
                        <h2 class="h5 fw-semibold mb-0"><i class="bi bi-flag-fill text-warning"></i> Challenge history</h2>
                        <c:if test="${status == 'SELF'}">
                            <form method="post" action="${ctx}/profile" class="m-0">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="action" value="history">
                                <c:choose>
                                    <c:when test="${profileUser.showChallengeHistory}">
                                        <button type="submit" name="history" value="hide" class="btn btn-sm btn-outline-light">
                                            <i class="bi bi-eye-slash"></i> Hide from my profile</button>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="submit" name="history" value="show" class="btn btn-sm btn-light text-primary">
                                            <i class="bi bi-eye"></i> Show on my profile</button>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </c:if>
                    </div>
                    <c:if test="${status == 'SELF'}">
                        <p class="small text-soft">
                            <c:choose>
                                <c:when test="${profileUser.showChallengeHistory}">Your friends can see this list on your profile.</c:when>
                                <c:otherwise><i class="bi bi-lock-fill"></i> Hidden: only you can see it. Hiding never deletes anything.</c:otherwise>
                            </c:choose>
                        </p>
                    </c:if>
                    <c:choose>
                        <c:when test="${empty challengeHistory}"><p class="mb-0 text-soft">No challenges yet.</p></c:when>
                        <c:otherwise>
                            <div class="scroll-box scroll-box-tall">
                            <c:forEach var="challenge" items="${challengeHistory}">
                                <div class="glass-inner p-2 px-3 mb-2 d-flex justify-content-between align-items-center gap-3 challenge-row">
                                    <div>
                                        <c:choose>
                                            <c:when test="${challenge.challengerId == profileUser.userId}">
                                                <i class="bi bi-arrow-up-right text-warning" title="Sent"></i> Challenged
                                                <a href="${ctx}/profile?id=${challenge.challengedId}" class="link-light"><c:out value="${challenge.challengedName}"/></a>
                                            </c:when>
                                            <c:otherwise>
                                                <i class="bi bi-arrow-down-left text-warning" title="Received"></i> Challenged by
                                                <a href="${ctx}/profile?id=${challenge.challengerId}" class="link-light"><c:out value="${challenge.challengerName}"/></a>
                                            </c:otherwise>
                                        </c:choose>
                                        to <a href="${ctx}/quiz?id=${challenge.quizId}" class="link-light fw-semibold"><c:out value="${challenge.quizTitle}"/></a>
                                        <div class="small text-soft"><fmt:formatDate value="${challenge.createdAt}" pattern="MMM d, yyyy"/></div>
                                    </div>
                                    <div class="text-end flex-shrink-0">
                                        <%@ include file="/WEB-INF/fragments/challenge-status.jspf" %>
                                        <c:if test="${challenge.score != null}">
                                            <div class="small mt-1"><strong>${challenge.score}/${challenge.totalQuestions}</strong> (${challenge.percentage}%)</div>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </c:if>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
