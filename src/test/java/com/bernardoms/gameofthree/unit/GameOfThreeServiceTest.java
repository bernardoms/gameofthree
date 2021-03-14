package com.bernardoms.gameofthree.unit;

import com.bernardoms.gameofthree.config.ApplicationConfig;
import com.bernardoms.gameofthree.event.WebSocketEventListener;
import com.bernardoms.gameofthree.exception.GameException;
import com.bernardoms.gameofthree.model.Game;
import com.bernardoms.gameofthree.model.Message;
import com.bernardoms.gameofthree.model.User;
import com.bernardoms.gameofthree.repository.GameRepository;
import com.bernardoms.gameofthree.service.GameOfThreeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameOfThreeServiceTest {

    private GameOfThreeService gameOfThreeService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ApplicationConfig applicationConfig;

    private static MockedStatic<WebSocketEventListener> webSocketEventListenerMockedStatic;
    @BeforeAll
    public static void setup() {
        webSocketEventListenerMockedStatic = Mockito.mockStatic(WebSocketEventListener.class);
    }

    @Test
    void testGameStartOnlyOnePlayer() {
        var message = new Message();
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(Collections.singletonList(new User("test", User.Role.ADMIN)));

        var exception = assertThrows(GameException.class, () -> gameOfThreeService.startGame(message));
        assertEquals("NEED 2 PLAYERS FOR START THE GAME", exception.getMessage());
    }

    @Test
    void testGameStartAlreadyInProgress() {
        var message = new Message();
        var game = new Game();
        game.setHasGameStarted(true);
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));
        when(gameRepository.getGame()).thenReturn(Collections.singletonList(game));

        var exception = assertThrows(GameException.class, () -> {
            gameOfThreeService.startGame(message);
        });
        assertEquals("A GAME IS ALREADY IN PROGRESS", exception.getMessage());
    }

    @Test
    void testStartGame() throws GameException {
        var message = new Message();
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));
        when(applicationConfig.getRangeNumber()).thenReturn(33);
        gameOfThreeService.startGame(message);
        assertNotNull(message.getContent());
    }

    @Test
    void waitingPlayer() {
        var message = new Message();
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>());
        gameOfThreeService.join(message);
        assertEquals("Waiting for another player", message.getContent());
    }

    @Test
    void join2Players() {
        var message = new Message();
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));
        gameOfThreeService.join(message);
        assertNull(message.getContent());
    }

    @Test
    void moveWrongValue() throws GameException {
        var message = new Message();
        message.setContent("6");
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);
        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));

        var exception = assertThrows(GameException.class, () -> {
            gameOfThreeService.move(message);
        });
        assertEquals("The number should be between -1 and 1", exception.getMessage());
    }

    @Test
    void moveGameNotStartedYet() {
        var message = new Message();
        message.setContent("-1");
        var game = new Game();
        game.setHasGameStarted(false);
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);

        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));

        when(gameRepository.getGame()).thenReturn(Collections.singletonList(game));

        var exception = assertThrows(GameException.class, () -> gameOfThreeService.move(message));
        assertEquals("You need to start the game", exception.getMessage());
    }

    @Test
    void moveGameStarted() throws GameException {
        var message = new Message();
        var game = new Game();
        game.setHasGameStarted(true);
        game.setGeneratedNumber(9);
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);

        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));

        when(applicationConfig.getRangeNumber()).thenReturn(33);
        gameOfThreeService.startGame(message);
        message.setContent("0");
        when(gameRepository.getGame()).thenReturn(Collections.singletonList(game));
        gameOfThreeService.move(message);
        assertEquals("(9 + 0)/3 = 3", message.getContent());
    }

    @Test
    void moveGameStartedWinningGame() throws GameException {
        var message = new Message();
        var game = new Game();
        game.setHasGameStarted(true);
        game.setGeneratedNumber(3);
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);

        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));

        when(applicationConfig.getRangeNumber()).thenReturn(33);
        gameOfThreeService.startGame(message);
        message.setContent("0");
        when(gameRepository.getGame()).thenReturn(Collections.singletonList(game));
        gameOfThreeService.move(message);
        assertEquals("(3 + 0)/3 = 1 WIN!!! Start the Game Again", message.getContent());
    }

    @Test
    void moveGamePlayer2Round() throws GameException {
        var message = new Message();
        message.setTo("player2");
        message.setSender("player1");
        var game = new Game();
        game.setPlayerTurn("player2");
        game.setHasGameStarted(true);
        gameOfThreeService = new GameOfThreeService(gameRepository, applicationConfig);

        webSocketEventListenerMockedStatic.when(WebSocketEventListener::getSessionList).thenReturn(new ArrayList<>(Arrays.asList(new User("test", User.Role.ADMIN), new User("test2", User.Role.USER))));

        when(applicationConfig.getRangeNumber()).thenReturn(33);
        gameOfThreeService.startGame(message);
        message.setContent("-1");
        when(gameRepository.getGame()).thenReturn(Collections.singletonList(game));

        var exception = assertThrows(GameException.class, () -> {
            gameOfThreeService.move(message);
        });
        assertEquals("player2 it's your turn to play", exception.getMessage());
    }
}
