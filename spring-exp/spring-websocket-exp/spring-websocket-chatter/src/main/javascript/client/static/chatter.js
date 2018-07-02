'use strict';

var connectStatus = document.querySelector('.connectStatus');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#messageInput');
var messageList = document.querySelector('#messageList');

var stompClient;


function init()
{
	var socket = new SockJS("");
	var stompLocal = Stomp.over(socket);
	stompLocal.connect(
		{},
		function() {
			stompLocal.subscribe("/topic/chatter/public/", onMessageReceived);
			connectStatus.textContent = "Connected";
			stompClient = stompLocal;
		},
		function() {
			connectStatus.textContent = "Connection failed";
		}
	);
	messageForm.addEventListener('submit', submitMessage, true);
}

function submitMessage(event)
{
	try {
		var content = messageInput.value.trim();
		if (content && stompClient) {
			stompClient.send("/app/chatter/postMessage", {}, JSON.stringify({
				content: content
			}));
		}
	}
	finally {
		event.preventDefault();
	}
}

function onMessageReceived(envelope)
{
	var message = JSON.parse(envelope.body);
	var tr = document.createElement("tr");
	var tdName = document.createElement("td");
	tdName.textContent = message.username;
	var tdTime = document.createElement("td");
	tdTime.textContent = new Date(message.time).toString();
	var tdContent = document.createElement("td");
	tdContent.textContent = message.content;
	tr.appendChild(tdName);
	tr.appendChild(tdTime);
	tr.appendChild(tdContent);
	messageList.appendChild(tr);
}