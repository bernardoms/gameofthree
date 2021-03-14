package com.bernardoms.gameofthree.controller;

import com.bernardoms.gameofthree.exception.GameException;
import com.bernardoms.gameofthree.model.Message;
import com.bernardoms.gameofthree.service.GameOfThreeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class GameController {

    private static final String TOPIC_TO_SEND = "/topic/";
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GameOfThreeService gameOfThreeService;

    @Autowired
    public GameController(SimpMessagingTemplate simpMessagingTemplate, GameOfThreeService gameOfThreeService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.gameOfThreeService = gameOfThreeService;
    }

    @MessageMapping("/join/room")
    public void joinRoom(@Payload Message chatMessage,
                         SimpMessageHeaderAccessor headerAccessor) {

        log.info("{} Entered in the room", chatMessage.getSender());

        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

        gameOfThreeService.join(chatMessage);

        simpMessagingTemplate.convertAndSend(TOPIC_TO_SEND, chatMessage);
    }

    @MessageMapping("/send/number")
    public void sendNumber(@Payload Message chatMessage) throws GameException {
        gameOfThreeService.move(chatMessage);
        simpMessagingTemplate.convertAndSend(TOPIC_TO_SEND, chatMessage);
    }

    @MessageMapping("/play")
    public void sendPlayMessage(@Payload Message chatMessage) throws GameException {
        gameOfThreeService.startGame(chatMessage);
        simpMessagingTemplate.convertAndSend(TOPIC_TO_SEND, chatMessage);
    }

    @MessageExceptionHandler
    public void handleException(Exception ex) {
        var message = new Message();
        message.setContent(ex.getMessage());
        message.setType(Message.MessageType.ERROR);
        simpMessagingTemplate.convertAndSend(TOPIC_TO_SEND, message);
    }
}
