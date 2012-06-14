<%@include file="../inc/doctype_html_admin.jsp" %>

<!-- Website designed and developed by Bright Interactive, http://www.bright-interactive.com -->
<%-- History:
--%>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/c.tld" prefix="c" %>
<%@ taglib uri="/WEB-INF/fmt.tld" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/bright-tag.tld" prefix="bright" %>

<head>
	<title><bright:cmsWrite identifier="company-name" filter="false" /> | Publishing Admin</title> 
	<%@include file="../inc/head-elements.jsp"%>
	<bean:define id="section" value="publishing"/>
	<bean:define id="pagetitle" value="Publishing"/>
</head>
<body id="adminPage">
	<%@include file="../inc/body_start.jsp"%>
	
	<h1 class="underline"><bean:write name="pagetitle" /></h1> 
 
	<h2>Manage Publishing Actions</h2> 
 
		<logic:notEmpty name="publishingForm" property="publishingActions" >
 		<table cellspacing="0" class="list" id="publishing-actions-list">
 			<thead>
	 			<tr>
	 				<th>Name</th>
					<th>Transfer Type</th>
	 				<th>Publish Location</th>	 				
	 				<th>Run Daily</th>
	 				<th colspan="3">Actions</th>
	 			</tr>
 			</thead>
 			<tbody>
 				<c:set var="publishActionIds" />
				<logic:iterate name="publishingForm" property="publishingActions" id="publishingAction">
					<tr id="publishing-action-<c:out value="${publishingAction.id}" />" class="publishing-action-item">
						<td class="item-name"><bean:write name="publishingAction" property="name" /></td>
						<td class="item-name"><bean:write name="publishingAction" property="fileTransferType" /></td>
						<td class="item-path"><bean:write name="publishingAction" property="path" /></td>						
						<td class="item-run-now"><c:if test="${publishingAction.runDaily}">yes</c:if><c:if test="${!publishingAction.runDaily}">no</c:if></td>
						<td class="action">[<a class="run-link" title="View all assets published by this action" href="runPublishingActionSearch?publishingActionId=<bean:write name="publishingAction" property="id" />" >view all matching assets</a>]</td>
						<td class="action">[<a class="run-link" title="Run this publishing action now" href="runPublishingAction?publishingActionId=<bean:write name="publishingAction" property="id" />&publishAll=1" onclick="if (!confirm('Are you sure you want to publish all assets for this action? This may take a long time')) return false;">publish all matching assets</a>]</td>
						<td class="action">[<a class="run-link" title="Run this publishing action now" href="runPublishingAction?publishingActionId=<bean:write name="publishingAction" property="id" />">publish changed assets</a>]</td>
						<td class="action">[<a class="edit-link" title="Modify this publishing action" href="viewAddPublishingAction?publishingActionId=<bean:write name="publishingAction" property="id" />">edit</a>]</td>
						<td class="action">[<a class="delete-link" title="Delete this publishing action" href="viewDeletePublishingAction?publishingActionId=<bean:write name="publishingAction" property="id" />" >X</a>]</td>
					</tr>
					
					<c:if test="${not empty publishActionIds}"><c:set var="publishActionIds" value="${publishActionIds},"/></c:if>
					<c:set var="publishActionIds" value="${publishActionIds}${publishingAction.id}"/>
				</logic:iterate>
				
				<tr style="border-bottom-style: hidden">
					<td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>
					<td class="action"><br/>[<a class="run-link" title="Run all publishing actions now" href="runPublishingAction?publishingActionId=${publishActionIds}&publishAll=1" onclick="if (!confirm('Are you sure you want to run all actions? This may take a long time')) return false;" >publish all matching assets for all actions</a>]</td>
					<td class="action"><br/>[<a class="run-link" title="Run all publishing actions now" href="runPublishingAction?publishingActionId=${publishActionIds}" >publish changed assets for all actions</a>]</td>
					<td>&nbsp;</td><td>&nbsp;</td>
				</tr>
			</tbody>
		</table>
	</logic:notEmpty>
		<logic:empty name="publishingForm" property="publishingActions" >
		<p>There are currently no Publishing Actions.</p> 
	</logic:empty>



	<p><a id="add-publishing-action-link" href="../action/viewAddPublishingAction">Add a new Publishing Action &raquo;</a></p>

	<%@include file="../inc/body_end.jsp"%>

</body>
</html>