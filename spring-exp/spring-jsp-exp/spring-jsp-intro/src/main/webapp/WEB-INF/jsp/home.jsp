<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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

	<nav class="navbar navbar-inverse">
		<div class="container">
			<div class="navbar-header">
				<a class="navbar-brand" href="#">Spring Jsp Intro</a>
			</div>
			<div id="navbar" class="collapse navbar-collapse">
				<ul class="nav navbar-nav">
					<li class="active"><a href="#">Home</a></li>
					<li><a href="#about">Login</a></li>
				</ul>
			</div>
		</div>
	</nav>


	<div class="container">
		<div class="starter-template">
			<h1>Spring Jsp Intro</h1>
			<h2>Message: ${message}</h2>
			<p>spring:message: <spring:message code="cz.znj.kvr.sw.exp.java.spring.jsp.Messages.greeting" /></p>
			<p>fmt:message: <fmt:message key="cz.znj.kvr.sw.exp.java.spring.jsp.Messages.javafmt" /></p>
			<p>spring:button:
			    <form:form method="POST">
			        <form:label path="message">Message</form:label><form:input path="message" />
			        <form:button name="button" value="cz.znj.kvr.sw.exp.java.spring.jsp.Messages.buttonText">Hello</form:button>
			    </form:form>
			</p>
		</div>

	</div>

</body>

</html>
