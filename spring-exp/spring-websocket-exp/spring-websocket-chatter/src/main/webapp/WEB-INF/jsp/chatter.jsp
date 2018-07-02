<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html lang="en">
<head>

	<spring:eval expression="@environment.getProperty('bootstrap.version')" var="bootstrapVersion" />
	<c:url value="/webjars/bootstrap/${bootstrapVersion}/css/bootstrap.min.css" var="bootstrapCss" />
	<link rel="stylesheet" type="text/css" href="${bootstrapCss}" />
	<spring:eval expression="@environment.getProperty('jquery.version')" var="jqueryVersion" />
	<c:url value="/webjars/jquery/${jqueryVersion}/jquery.min.js" var="jqueryJs" />
	<script type='text/javascript' src="${jqueryJs}"></script>
	<spring:eval expression="@environment.getProperty('sockjs-client.version')" var="sockjsclientVersion" />
	<c:url value="/webjars/sockjs-client/${sockjsclientVersion}/sockjs.min.js" var="sockjsclientJs" />
	<script type='text/javascript' src="${sockjsclientJs}"></script>
	<spring:eval expression="@environment.getProperty('stomp-websocket.version')" var="stompwebsocketVersion" />
	<c:url value="/webjars/stomp-websocket/${stompwebsocketVersion}/stomp.min.js" var="stompwebsocketJs" />
	<script type='text/javascript' src="${stompwebsocketJs}"></script>

</head>
<body onload="init();">

	<nav class="navbar navbar-inverse">
		<div class="container">
			<div class="navbar-header">
				<a class="navbar-brand" href="#">Spring Websocket Chatter</a>
			</div>
			<div id="navbar" class="navbar-collapse">
				<ul class="nav navbar-nav">
					<li><a href="../">Home</a></li>
					<li class="active"><a href="../chatter/">Chatter</a></li>
					<li><a href="../login/?redirect=../chatter/">Login</a></li>
				</ul>
			</div>
		</div>
	</nav>


	<div class="container">
		<div class="starter-template">
			<h1>Spring Websocket Chatter</h1>
		</div>
	</div>

	<div class="connectStatus">
		Connecting
	</div>

	<form id="messageForm" name="messageForm">
		<input type="text" id="messageInput" placeholder="Type a message..." autocomplete="off" />
		<button type="submit" class="primary">Send</button>
	</form>

	<table id="messageList">
	</table>

	<c:url value="/static/chatter.js" var="chatterJs" />
	<script type='text/javascript' src="${chatterJs}"></script>

</body>

</html>
