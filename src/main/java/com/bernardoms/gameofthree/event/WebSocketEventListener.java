package com.bernardoms.gameofthree.event;

import com.bernardoms.gameofthree.model.Message;
import com.bernardoms.gameofthree.model.User;
import com.bernardoms.gameofthree.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.LinkedList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messagingTemplate;
    private static final List<User> sessionList = new LinkedList<>();
    private final GameRepository gameRepository;

    public static List<User> getSessionList() {
        return sessionList;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        var username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            log.info("User Disconnected : {}", username);
            var chatMessage = new Message();
            chatMessage.setType(Message.MessageType.LEAVE);
            chatMessage.setSender(username);
            messagingTemplate.convertAndSend("/topic/", chatMessage);
            gameRepository.getGame().stream().findFirst().ifPresent(gameRepository::deleteGame);
            sessionList.removeIf(user -> user.getUsername().equals(username));
            if (sessionList.size() == 1) {
                sessionList.get(0).setRole(User.Role.ADMIN);
                chatMessage.setContent(chatMessage.getSender() + "Is now the admin of the room!");
            }
        }
    }
}
