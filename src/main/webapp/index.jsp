<%-- The site's front door: logged-in users go to their dashboard (AuthFilter sends everyone else to /login). --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:redirect url="/home"/>
