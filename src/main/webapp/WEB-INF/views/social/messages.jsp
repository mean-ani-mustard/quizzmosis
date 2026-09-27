<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Messages"/>
<c:set var="activeNav" value="messages"/>
<c:set var="myId" value="${sessionScope.user.userId}"/>
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
        <h1 class="display-6 fw-bold">Messages</h1>
        <p class="lead mb-0">Friend requests, quiz challenges and notes from your friends.</p>
    </header>

    <%-- Tabs: the servlet decides which one is open (e.g. ?tab=notes after sending a note). --%>
    <c:set var="challengeNews" value="${fn:length(pendingChallenges) + unreadUpdates}"/>
    <ul class="nav nav-pills justify-content-center row-gap-2 mb-4" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link ${tab == 'requests' ? 'active' : ''}" data-bs-toggle="pill" data-bs-target="#tab-requests" type="button" role="tab">
                <i class="bi bi-person-plus-fill"></i> Friend requests
                <c:if test="${not empty requests}"><span class="badge bg-danger">${fn:length(requests)}</span></c:if>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link ${tab == 'challenges' ? 'active' : ''}" data-bs-toggle="pill" data-bs-target="#tab-challenges" type="button" role="tab">
                <i class="bi bi-flag-fill"></i> Challenges
                <c:if test="${challengeNews > 0}"><span class="badge bg-danger">${challengeNews}</span></c:if>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link ${tab == 'notes' ? 'active' : ''}" data-bs-toggle="pill" data-bs-target="#tab-notes" type="button" role="tab">
                <i class="bi bi-chat-left-text-fill"></i> Notes
                <c:if test="${unreadThreads > 0}"><span class="badge bg-danger">${unreadThreads}</span></c:if>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link ${tab == 'history' ? 'active' : ''}" data-bs-toggle="pill" data-bs-target="#tab-history" type="button" role="tab">
                <i class="bi bi-clock-history"></i> Challenge history
            </button>
        </li>
    </ul>

    <div class="tab-content glass-card p-3 p-md-4">
        <%-- Friend requests --%>
        <div class="tab-pane fade ${tab == 'requests' ? 'show active' : ''}" id="tab-requests" role="tabpanel">
            <c:if test="${empty requests}"><p class="mb-0 text-center text-soft">No open friend requests.</p></c:if>
            <c:forEach var="person" items="${requests}">
                <div class="glass-inner p-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <span>
                        <q:avatar name="${person.displayName}" userId="${person.userId}" size="md"/>
                        <a href="${ctx}/profile?id=${person.userId}" class="link-light fw-semibold"><c:out value="${person.displayName}"/></a>
                        wants to be your friend.
                    </span>
                    <form method="post" action="${ctx}/friends" class="m-0 d-flex gap-2">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="userId" value="${person.userId}">
                        <input type="hidden" name="returnTo" value="messages">
                        <button type="submit" name="action" value="accept" class="btn btn-sm btn-success"><i class="bi bi-check"></i> Accept</button>
                        <button type="submit" name="action" value="reject" class="btn btn-sm btn-outline-light"><i class="bi bi-x"></i> Decline</button>
                    </form>
                </div>
            </c:forEach>
        </div>

        <%-- Challenges: the ones waiting for an answer, then what friends did with the ones you sent. --%>
        <div class="tab-pane fade ${tab == 'challenges' ? 'show active' : ''}" id="tab-challenges" role="tabpanel">
            <h2 class="h6 fw-semibold mb-3"><i class="bi bi-inbox-fill"></i> Waiting for your answer</h2>
            <c:if test="${empty pendingChallenges}"><p class="text-soft">No challenges are waiting for you.</p></c:if>
            <c:forEach var="challenge" items="${pendingChallenges}">
                <div class="glass-inner p-3 mb-3" id="challenge-${challenge.challengeId}">
                    <div class="d-flex justify-content-between flex-wrap gap-2">
                        <div>
                            <q:avatar name="${challenge.challengerName}" userId="${challenge.challengerId}"/>
                            <a href="${ctx}/profile?id=${challenge.challengerId}" class="link-light fw-semibold"><c:out value="${challenge.challengerName}"/></a>
                            challenged you to
                            <a href="${ctx}/quiz?id=${challenge.quizId}" class="link-light fw-semibold"><c:out value="${challenge.quizTitle}"/></a>
                        </div>
                        <small class="text-soft"><fmt:formatDate value="${challenge.createdAt}" pattern="MMM d, HH:mm"/></small>
                    </div>
                    <p class="mb-2 mt-1 fst-italic pre-wrap">&ldquo;<c:out value="${challenge.message}"/>&rdquo;</p>
                    <p class="small mb-2">
                        Their best:
                        <c:choose>
                            <c:when test="${challenge.challengerBestPercent == null}">not taken yet</c:when>
                            <c:otherwise><strong>${challenge.challengerBestPercent}%</strong></c:otherwise>
                        </c:choose>
                    </p>
                    <%-- One form, two buttons: the comment goes to the challenger with either answer. --%>
                    <form method="post" action="${ctx}/messages" class="m-0">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="challengeId" value="${challenge.challengeId}">
                        <label for="comment-${challenge.challengeId}" class="visually-hidden">Comment for <c:out value="${challenge.challengerName}"/></label>
                        <div class="d-flex flex-wrap gap-2">
                            <input type="text" id="comment-${challenge.challengeId}" name="comment" maxlength="500"
                                   class="form-control form-control-sm challenge-comment"
                                   placeholder="Add a comment for ${fn:escapeXml(challenge.challengerName)} (optional)">
                            <button type="submit" name="action" value="accept" class="btn btn-sm btn-warning fw-semibold">
                                <i class="bi bi-play-fill"></i> Accept &amp; play</button>
                            <button type="submit" name="action" value="decline" class="btn btn-sm btn-outline-light">
                                <i class="bi bi-x-lg"></i> Decline</button>
                        </div>
                    </form>
                </div>
            </c:forEach>

            <h2 class="h6 fw-semibold mt-4 mb-3"><i class="bi bi-reply-all-fill"></i> Answers to your challenges</h2>
            <c:if test="${empty challengeUpdates}"><p class="mb-0 text-soft">Nobody has answered your challenges yet.</p></c:if>
            <c:forEach var="update" items="${challengeUpdates}">
                <div class="glass-inner p-3 mb-2 ${update.read ? '' : 'unread'}">
                    <div class="d-flex justify-content-between flex-wrap gap-2">
                        <div>
                            <c:if test="${not update.read}"><span class="badge bg-warning text-dark me-1">New</span></c:if>
                            <q:avatar name="${update.senderDisplayName}" userId="${update.senderId}"/>
                            <a href="${ctx}/profile?id=${update.senderId}" class="link-light fw-semibold"><c:out value="${update.senderDisplayName}"/></a>
                            <c:choose>
                                <c:when test="${update.type == 'CHALLENGE_ACCEPTED'}">
                                    <span class="badge bg-info text-dark"><i class="bi bi-play-circle"></i> accepted</span> your challenge to</c:when>
                                <c:when test="${update.type == 'CHALLENGE_DECLINED'}">
                                    <span class="badge bg-danger"><i class="bi bi-x-circle"></i> declined</span> your challenge to</c:when>
                                <c:otherwise>
                                    <span class="badge bg-success"><i class="bi bi-check-circle"></i> finished</span> your challenge to</c:otherwise>
                            </c:choose>
                            <a href="${ctx}/quiz?id=${update.quizId}" class="link-light fw-semibold"><c:out value="${update.quizTitle}"/></a>
                        </div>
                        <small class="text-soft"><fmt:formatDate value="${update.sentAt}" pattern="MMM d, HH:mm"/></small>
                    </div>
                    <c:if test="${update.type == 'CHALLENGE_FINISHED' and update.score != null}">
                        <p class="mb-0 mt-2">
                            Their score: <strong>${update.score}/${update.totalQuestions}</strong> (${update.percentage}%)
                            <span class="text-soft ms-2">&middot; your best:
                                <c:choose>
                                    <c:when test="${update.receiverBestPercent == null}">not taken yet</c:when>
                                    <c:otherwise>${update.receiverBestPercent}%</c:otherwise>
                                </c:choose>
                            </span>
                        </p>
                    </c:if>
                    <c:if test="${not empty update.content}">
                        <p class="mb-0 mt-2 pre-wrap"><i class="bi bi-chat-quote-fill text-warning"></i> &ldquo;<c:out value="${update.content}"/>&rdquo;</p>
                    </c:if>
                    <c:if test="${update.type == 'CHALLENGE_FINISHED' and not empty update.resultComment}">
                        <p class="mb-0 mt-2 pre-wrap"><i class="bi bi-chat-quote-fill text-warning"></i> Comment on the quiz:
                            &ldquo;<c:out value="${update.resultComment}"/>&rdquo;</p>
                    </c:if>
                </div>
            </c:forEach>
        </div>

        <%-- Notes: conversations with friends. Replies appear underneath the first note. --%>
        <div class="tab-pane fade ${tab == 'notes' ? 'show active' : ''}" id="tab-notes" role="tabpanel">
            <c:choose>
                <c:when test="${empty friends}">
                    <p class="text-soft">Add some friends to send them notes.</p>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/messages" class="glass-inner p-3 mb-4">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="action" value="send_note">
                        <h2 class="h6 fw-semibold">Start a conversation</h2>
                        <div class="row g-2">
                            <div class="col-md-4">
                                <label for="receiverId" class="visually-hidden">To</label>
                                <select name="receiverId" id="receiverId" class="form-select" required>
                                    <option value="">To...</option>
                                    <c:forEach var="friend" items="${friends}">
                                        <option value="${friend.userId}" ${friend.userId == noteTo ? 'selected' : ''}><c:out value="${friend.displayName}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label for="content" class="visually-hidden">Message</label>
                                <textarea name="content" id="content" class="form-control" rows="1" maxlength="1000"
                                          placeholder="Write a note..." required></textarea>
                            </div>
                            <div class="col-md-2">
                                <button type="submit" class="btn btn-light text-primary w-100"><i class="bi bi-send"></i> Send</button>
                            </div>
                        </div>
                    </form>
                </c:otherwise>
            </c:choose>

            <c:if test="${empty threads}"><p class="mb-0 text-center text-soft">No notes yet.</p></c:if>
            <c:forEach var="thread" items="${threads}">
                <article class="glass-inner p-3 mb-3 ${thread.unread ? 'unread' : ''}" id="thread-${thread.threadId}">
                    <h3 class="h6 fw-semibold mb-3">
                        <c:if test="${thread.unread}"><span class="badge bg-warning text-dark me-1">New</span></c:if>
                        <q:avatar name="${thread.otherDisplayName}" userId="${thread.otherUserId}" size="md"/> With
                        <a href="${ctx}/profile?id=${thread.otherUserId}" class="link-light"><c:out value="${thread.otherDisplayName}"/></a>
                    </h3>
                    <div class="note-thread">
                        <c:forEach var="note" items="${thread.messages}">
                            <c:set var="mine" value="${note.senderId == myId}"/>
                            <div class="note ${mine ? 'note-mine' : ''}">
                                <div class="d-flex justify-content-between gap-2 small mb-1">
                                    <span class="fw-semibold">
                                        <c:choose>
                                            <c:when test="${mine}">You</c:when>
                                            <c:otherwise><c:out value="${note.senderDisplayName}"/></c:otherwise>
                                        </c:choose>
                                        <c:if test="${not mine and not note.read}"><span class="badge bg-warning text-dark ms-1">New</span></c:if>
                                    </span>
                                    <span class="text-soft"><fmt:formatDate value="${note.sentAt}" pattern="MMM d, HH:mm"/></span>
                                </div>
                                <div class="pre-wrap"><c:out value="${note.content}"/></div>
                            </div>
                        </c:forEach>
                    </div>
                    <c:choose>
                        <c:when test="${thread.replyAllowed}">
                            <form method="post" action="${ctx}/messages" class="mt-3">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="action" value="reply">
                                <input type="hidden" name="threadId" value="${thread.threadId}">
                                <label for="reply-${thread.threadId}" class="visually-hidden">Reply to <c:out value="${thread.otherDisplayName}"/></label>
                                <div class="input-group">
                                    <textarea id="reply-${thread.threadId}" name="content" class="form-control" rows="1" maxlength="1000"
                                              placeholder="Write a reply..." required></textarea>
                                    <button type="submit" class="btn btn-light text-primary"><i class="bi bi-reply-fill"></i> Reply</button>
                                </div>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="small text-soft mb-0 mt-3"><i class="bi bi-lock-fill"></i> You can only reply to your friends.</p>
                        </c:otherwise>
                    </c:choose>
                </article>
            </c:forEach>
        </div>

        <%-- Every challenge sent or received. Challenges are never deleted. --%>
        <div class="tab-pane fade ${tab == 'history' ? 'show active' : ''}" id="tab-history" role="tabpanel">
            <p class="small text-soft">
                Every challenge you sent or received. Nothing here is ever deleted; on
                <a href="${ctx}/profile#challenge-history" class="link-light">your profile</a> you choose whether your friends can see it.
            </p>
            <c:choose>
                <c:when test="${empty history}"><p class="mb-0 text-center text-soft">No challenges yet.</p></c:when>
                <c:otherwise>
                    <c:forEach var="challenge" items="${history}">
                        <c:set var="sent" value="${challenge.challengerId == myId}"/>
                        <div class="glass-inner p-3 mb-2 challenge-row">
                            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                                <div>
                                    <c:choose>
                                        <c:when test="${sent}">
                                            <i class="bi bi-arrow-up-right text-warning" title="Sent"></i> You challenged
                                            <a href="${ctx}/profile?id=${challenge.challengedId}" class="link-light"><c:out value="${challenge.challengedName}"/></a>
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-arrow-down-left text-warning" title="Received"></i>
                                            <a href="${ctx}/profile?id=${challenge.challengerId}" class="link-light"><c:out value="${challenge.challengerName}"/></a> challenged you
                                        </c:otherwise>
                                    </c:choose>
                                    to <a href="${ctx}/quiz?id=${challenge.quizId}" class="link-light fw-semibold"><c:out value="${challenge.quizTitle}"/></a>
                                    <div class="small text-soft fst-italic">&ldquo;<c:out value="${challenge.message}"/>&rdquo;</div>
                                </div>
                                <div class="d-flex align-items-center flex-wrap gap-2">
                                    <c:if test="${challenge.score != null}">
                                        <span class="text-nowrap"><strong>${challenge.score}/${challenge.totalQuestions}</strong>
                                            <span class="small text-soft">(${challenge.percentage}%)</span></span>
                                    </c:if>
                                    <%@ include file="/WEB-INF/fragments/challenge-status.jspf" %>
                                    <c:if test="${not sent and challenge.status == 'PENDING'}">
                                        <a href="${ctx}/messages?tab=challenges#challenge-${challenge.challengeId}" class="btn btn-sm btn-outline-light">Answer</a>
                                    </c:if>
                                    <c:if test="${not sent and challenge.status == 'ACCEPTED'}">
                                        <a href="${ctx}/quiz/start?id=${challenge.quizId}" class="btn btn-sm btn-warning"><i class="bi bi-play-fill"></i> Play</a>
                                    </c:if>
                                </div>
                            </div>
                            <div class="small text-soft mt-1">
                                Sent <fmt:formatDate value="${challenge.createdAt}" pattern="MMM d, yyyy"/>
                                <c:if test="${not empty challenge.respondedAt}">
                                    &middot; ${challenge.status == 'DECLINED' ? 'declined' : 'accepted'}
                                    <fmt:formatDate value="${challenge.respondedAt}" pattern="MMM d, yyyy"/>
                                </c:if>
                                <c:if test="${not empty challenge.completedAt}">
                                    &middot; finished <fmt:formatDate value="${challenge.completedAt}" pattern="MMM d, yyyy"/>
                                </c:if>
                            </div>
                            <c:if test="${not empty challenge.responseComment}">
                                <div class="small mt-1"><i class="bi bi-chat-quote-fill text-warning"></i>
                                    ${sent ? 'Their answer' : 'Your answer'}: &ldquo;<c:out value="${challenge.responseComment}"/>&rdquo;</div>
                            </c:if>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
</body>
</html>
