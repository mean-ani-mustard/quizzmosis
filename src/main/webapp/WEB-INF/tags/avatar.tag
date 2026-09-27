<%@ tag description="A round avatar: someone's initials on a colour of their own" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" description="The display name, e.g. Alice Johnson (shown as AJ)" %>
<%@ attribute name="userId" required="true" type="java.lang.Integer" description="Picks the colour" %>
<%@ attribute name="size" description="sm (the default), md, lg or xl" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="qf" uri="/WEB-INF/quizzmosis.tld" %>
<%-- The colour comes from the user id, so everyone has the same colour on every page. --%>
<span class="avatar avatar-${empty size ? 'sm' : size} avatar-color-${userId % 8}"
      aria-hidden="true">${fn:escapeXml(qf:initials(name))}</span>
