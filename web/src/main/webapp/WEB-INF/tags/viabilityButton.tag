<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<%@ attribute name="callList" required="true" type="java.util.Set"%>
<%@ attribute name="link" required="true" type="java.lang.String"%>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>



<c:if test="${callList.size() == 1}">
	<!--<a class="status done" href="${link}">	-->
		<c:forEach var="call" items="${callList}" varStatus="loop">
			<%--<span class="left">${call.replaceAll("Homozygous - ","Hom<br/>")}</span>--%>
			<c:if test="${fn:contains(call, 'Lethal')}">
				<span class="badge badge-danger">${call}</span>
			</c:if>
			<c:if test="${fn:contains(call, 'Subviable')}">
				<span class="badge badge-warning">${call}</span>
			</c:if>
		</c:forEach>
	<!--</a> -->
</c:if>

<c:if test="${callList.size() > 1}">
	<!--<a  href="${link}" class="status done" title="Conflicting calls were made for this gene. For details refer to the associations table on the gene page.">-->
	<!-- span  class="status done" title="Conflicting calls were made for this gene. For details refer to the associations table on the gene page."-->
		<c:forEach var="call" items="${callList}" varStatus="loop">
			<%--<span class="left">${call.replaceAll("Homozygous - ","Hom<br/>")}</span>--%>
			<c:if test="${fn:contains(call, 'Lethal')}">
				<span class="badge badge-danger">${call}</span>
			</c:if>
			<c:if test="${fn:contains(call, 'Subviable')}">
				<span class="badge badge-warning">${call}</span>
			</c:if>
		</c:forEach>
	<!--/span-->
</c:if>