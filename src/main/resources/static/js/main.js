'use strict';

const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const messageArea = document.querySelector('#messageArea');
const connectingElement = document.querySelector('.connecting');
const playButton = document.getElementById("play-button");
const checkAutomatic = document.querySelector("input[name=checkbox]");

let isAutomatic = false;
let stompClient = null;
let username = null;
let rival = null;

const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        const socket = new SockJS('/app');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}


function onConnected() {
    const users = getUser();
    if (users.length >= 2) {
        connectingElement.textContent = 'The room is full!!';
        connectingElement.style.color = 'red';
        return;
    }
    stompClient.subscribe('/topic/', onMessageReceived);

    stompClient.send("/game-of-three/join/room", {}, JSON.stringify({sender: username, type: 'JOIN'}));

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}


function sendChatMessage(event) {
    const messageContent = messageInput.value.trim();

    if (messageContent && stompClient) {
        const chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'PLAY',
            to: rival
        };

        stompClient.send("/game-of-three/send/message", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function sendGameMessage(event, value) {
    if (value !== undefined && stompClient) {
        const chatMessage = {
            sender: username,
            content: value,
            type: 'PLAY',
            to: rival
        };

        stompClient.send("/game-of-three/send/number", {}, JSON.stringify(chatMessage));
    }
    event.preventDefault();
}

function play(event) {
    if (stompClient) {
        const chatMessage = {
            sender: username,
            type: 'START',
            to: rival
        };
        stompClient.send("/game-of-three/play", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    const messageElement = document.createElement('li');
    const users = getUser();

    const user = users.filter(function (user) {
        return user.username === username
    });

    if (user[0].role !== "ADMIN") {
        playButton.style.visibility = "hidden";
    } else {
        playButton.style.visibility = "visible";
    }

    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        if (message.content) {
            message.content = message.sender + ' joined! ' + message.content;
        } else {
            rival = users.filter(function (user) {
                return user.username !== username;
            })[0].username;
            message.content = message.sender + ' joined! The game can start now!';
        }
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!'
    } else if (message.type === 'ERROR') {
        messageElement.classList.add('event-message');
    } else {

        if (isAutomatic && message.to === username && message.type !== "WON") {
            if (stompClient) {
                const chatMessage = {
                    sender: username,
                    content: generateRandomNumber(1, -1),
                    type: 'PLAY',
                    to: rival
                };
                stompClient.send("/game-of-three/send/number", {}, JSON.stringify(chatMessage));
                messageInput.value = '';
            }
        }

        messageElement.classList.add('chat-message');

        const avatarElement = document.createElement('i');
        const avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        const usernameElement = document.createElement('span');
        const usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    const textElement = document.createElement('p');
    const messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function getAvatarColor(messageSender) {
    let hash = 0;
    for (let i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    const index = Math.abs(hash % colors.length);
    return colors[index];
}

function getUser() {
    const xhttp = new XMLHttpRequest();
    xhttp.open("GET", "/user", false);
    xhttp.setRequestHeader("Content-type", "application/json");
    xhttp.send();
    return JSON.parse(xhttp.responseText);
}

function generateRandomNumber(maximum, minimum) {
    return Math.floor(Math.random() * (maximum - minimum + 1)) + minimum;
}

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendChatMessage, true);
messageForm.addEventListener('submit', sendGameMessage, true);
checkAutomatic.addEventListener('change', function () {
    isAutomatic = this.checked;
});
