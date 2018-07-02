<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<html lang="en">
<head>

	<spring:eval expression="@environment.getProperty('bootstrap.version')" var="bootstrapVersion" />
	<c:url value="/webjars/bootstrap/${bootstrapVersion}/css/bootstrap.min.css" var="bootstrapCss" />
	<link rel="stylesheet" type="text/css" href="${bootstrapCss}" />
	<spring:eval expression="@environment.getProperty('jquery.version')" var="jqueryVersion" />
	<c:url value="/webjars/jquery/${jqueryVersion}/jquery.min.js" var="jqueryJs" />
	<script type='text/javascript' src="${jqueryJs}"></script>

</head>
<body>


	<form:form method="POST" action="." modelAttribute="loginForm">
		<form:hidden path="redirect" />
		<table>
			<tr>
				<td><form:label path="name">Name</form:label></td>
				<td><form:input path="name"/><form:errors path="name" /></td>
			</tr>
			<tr>
				<td><input type="submit" value="Submit"/></td>
			</tr>
		</table>
	</form:form>

</body>

</html>
