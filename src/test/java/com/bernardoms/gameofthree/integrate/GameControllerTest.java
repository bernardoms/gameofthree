package com.bernardoms.gameofthree.integrate;

import com.bernardoms.gameofthree.event.WebSocketEventListener;
import com.bernardoms.gameofthree.model.Game;
import com.bernardoms.gameofthree.model.Message;
import com.bernardoms.gameofthree.model.User;
import com.bernardoms.gameofthree.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerTest {
    @Value("${local.server.port}")
    private int port;
    private static String WEBSOCKET_URI;
    private static final String WEBSOCKET_TOPIC = "/topic/";
    private static final String GAME_ROOM_ENDPOINT = "/game-of-three/join/room";
    private static final String PLAY_ENDPOINT = "/game-of-three/play";
    private static final String SEND_NUMBER = "/game-of-three/send/number";
    private CompletableFuture<Message> completableFuture;
    private CompletableFuture<Message> completableFuture2;

    @Autowired
    private GameRepository gameRepository;

    @BeforeEach
    public void setup() {
        completableFuture = new CompletableFuture<>();
        completableFuture2 = new CompletableFuture<>();
        WEBSOCKET_URI = "ws://localhost:" + port + "/app";
        WebSocketEventListener.getSessionList().removeAll(WebSocketEventListener.getSessionList());
    }

    @Test
    void shouldJoinGameRoomTwoSubscribers() throws Exception {

        var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        var session = stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new FirstSessionHandler());

        var message = new Message();
        message.setType(Message.MessageType.JOIN);
        message.setSender("TEST");

        session.send(GAME_ROOM_ENDPOINT, message);

        var responseMessage = completableFuture.get(10, SECONDS);

        assertEquals(message.getSender(), responseMessage.getSender());

        assertEquals("Waiting for another player", responseMessage.getContent());

        assertEquals(message.getType(), responseMessage.getType());

        stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new SecondSessionHandler());

        var message2 = new Message();
        message2.setType(Message.MessageType.JOIN);
        message2.setSender("TEST2");
        message2.setContent("TEST MESSAGE 2");
        session.send(GAME_ROOM_ENDPOINT, message2);

        var responseMessage2 = completableFuture2.get(10, SECONDS);

        assertEquals(message2.getSender(), responseMessage2.getSender());

        assertNotEquals("Waiting for another player", responseMessage2.getContent());

        assertEquals(message2.getType(), responseMessage2.getType());
    }

    @Test
    void shouldPlayWithSuccess() throws InterruptedException, ExecutionException, TimeoutException {
        var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new FirstSessionHandler());

        WebSocketEventListener.getSessionList().add(new User("TEST", User.Role.ADMIN));
        WebSocketEventListener.getSessionList().add(new User("TEST2", User.Role.USER));

        var message = new Message();
        message.setType(Message.MessageType.JOIN);
        message.setSender("TEST");

        session.send(PLAY_ENDPOINT, message);

        var responseMessage = completableFuture.get(10, SECONDS);

        assertEquals(message.getSender(), responseMessage.getSender());

        assertNotNull(responseMessage.getContent());

        assertEquals("START", responseMessage.getType().name());
    }


    @Test
    void shouldReturnNeed2PlayersMessageWhenThereIsOnlyOnePlayerWhenStartGame() throws InterruptedException, ExecutionException, TimeoutException {
        var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        var session = stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new FirstSessionHandler());

        WebSocketEventListener.getSessionList().add(new User("TEST", User.Role.ADMIN));

        var message = new Message();
        message.setType(Message.MessageType.JOIN);
        message.setSender("TEST");

        session.send(PLAY_ENDPOINT, message);

        var responseMessage = completableFuture.get(10, SECONDS);

        assertNotNull(responseMessage.getContent());

        assertEquals(Message.MessageType.ERROR, responseMessage.getType());
        assertEquals("NEED 2 PLAYERS FOR START THE GAME", responseMessage.getContent());
    }

    @Test
    void shouldReturnGameIsInProgressWhenTryToStartAStartedMatch() throws InterruptedException, ExecutionException, TimeoutException {
        var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        var game = new Game();

        gameRepository.saveGame(game);

        var session = stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new FirstSessionHandler());

        WebSocketEventListener.getSessionList().add(new User("TEST", User.Role.ADMIN));
        WebSocketEventListener.getSessionList().add(new User("TEST2", User.Role.USER));

        var message = new Message();
        message.setType(Message.MessageType.JOIN);
        message.setSender("TEST");

        session.send(PLAY_ENDPOINT, message);

        var responseMessage = completableFuture.get(10, SECONDS);

        gameRepository.deleteGame(game);

        assertNotNull(responseMessage.getContent());

        System.out.println(WebSocketEventListener.getSessionList());

        assertEquals(Message.MessageType.ERROR, responseMessage.getType());
        assertEquals("A GAME IS ALREADY IN PROGRESS", responseMessage.getContent());
    }

    @Test
    void shouldSendNumberWithSuccess() throws InterruptedException, ExecutionException, TimeoutException {
        var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        var session = stompClient
                .connect(WEBSOCKET_URI, new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe(WEBSOCKET_TOPIC, new FirstSessionHandler());

        WebSocketEventListener.getSessionList().add(new User("TEST", User.Role.ADMIN));
        WebSocketEventListener.getSessionList().add(new User("TEST2", User.Role.USER));

        var game = new Game();
        game.setHasGameStarted(true);
        game.setGeneratedNumber(33);

        gameRepository.saveGame(game);

        var message = new Message();
        message.setSender("TESTE2");
        message.setContent("-1");
        session.send(SEND_NUMBER, message);

        var responseMessage = completableFuture.get(10, SECONDS);

        gameRepository.deleteGame(game);

        assertEquals(message.getSender(), responseMessage.getSender());

        assertNotNull(responseMessage.getContent());

        assertEquals("PLAY", responseMessage.getType().name());
    }

    private List<Transport> createTransportClient() {
        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return transports;
    }

    class FirstSessionHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return Message.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            completableFuture.complete((Message) o);
        }
    }

    class SecondSessionHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return Message.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            completableFuture2.complete((Message) o);
        }
    }

}
