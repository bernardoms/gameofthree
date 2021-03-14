package com.bernardoms.gameofthree.service;


import com.bernardoms.gameofthree.config.ApplicationConfig;
import com.bernardoms.gameofthree.event.WebSocketEventListener;
import com.bernardoms.gameofthree.exception.GameException;
import com.bernardoms.gameofthree.model.Game;
import com.bernardoms.gameofthree.model.Message;
import com.bernardoms.gameofthree.model.User;
import com.bernardoms.gameofthree.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class GameOfThreeService {
    private final GameRepository gameRepository;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public GameOfThreeService(GameRepository gameRepository, ApplicationConfig applicationConfig) {
        this.gameRepository = gameRepository;
        this.applicationConfig = applicationConfig;
    }

    public void startGame(Message message) throws GameException {
        if (WebSocketEventListener.getSessionList().size() < 2) {
            log.error("NEED 2 PLAYERS FOR START THE GAME");
            throw new GameException("NEED 2 PLAYERS FOR START THE GAME");
        } else if (gameRepository.getGame().stream().findFirst().isPresent()) {
            log.error("A GAME IS ALREADY IN PROGRESS");
            throw new GameException("A GAME IS ALREADY IN PROGRESS");
        } else {
            message.setType(Message.MessageType.START);

            int generatedNumber = generateNumber();

            var game = new Game();
            game.setHasGameStarted(true);
            game.setUsers(WebSocketEventListener.getSessionList());
            game.setGeneratedNumber(generatedNumber);
            game.setPlayerTurn(message.getTo());
            gameRepository.saveGame(game);

            message.setContent(String.valueOf(generatedNumber));
        }
    }

    private Integer generateNumber() {
        var random = new Random();
        int generatedNumber = random.nextInt(applicationConfig.getRangeNumber()) * 3;
        if (generatedNumber == 0) {
            generatedNumber = random.nextInt(applicationConfig.getRangeNumber()) * 3;
        }
        return generatedNumber;
    }

    public void move(Message message) throws GameException {
        var gameOptional = gameRepository.getGame().stream().findFirst();

        if (!message.isValidMessage()) {
            throw new GameException("The number should be between -1 and 1");
        }

        if (gameOptional.isEmpty() || !gameOptional.get().isHasGameStarted()) {
            throw new GameException("You need to start the game");
        }

        if (WebSocketEventListener.getSessionList().size() < 2) {
            throw new GameException("You need to wait for a other player to join");
        }

        if (gameOptional.get().getPlayerTurn() != null && !gameOptional.get().getPlayerTurn().equals(message.getSender())) {
            throw new GameException(message.getTo() + " it's your turn to play");
        }

        message.setType(Message.MessageType.PLAY);
        gameOptional.get().setPlayerTurn(message.getTo());
        gameRepository.updateGame(gameOptional.get());
        message.setContent(calculate(Integer.parseInt(message.getContent()), message, gameOptional.get()));
    }

    private String calculate(int number, Message message, Game game) {
        var generatedNumber = game.getGeneratedNumber();
        int aux;
        var sum = game.sumToGeneratedNumber(number);
        if ((sum % 3) != 0) {
            return "Wrong Move, " + number + " + " + generatedNumber + " = " + sum + " is not divisible by 3";
        } else {
            aux = generatedNumber;
            generatedNumber = sum / 3;
            game.setGeneratedNumber(generatedNumber);
        }
        if (generatedNumber == 1) {
            message.setType(Message.MessageType.WON);
            gameRepository.getGame().stream().findFirst().ifPresent(gameRepository::deleteGame);
            return "(" + aux + " + " + number + ")" + "/3" + " = " + generatedNumber + " WIN!!! Start the Game Again";
        }
        return "(" + aux + " + " + number + ")" + "/3" + " = " + generatedNumber;
    }

    public void join(Message message) {
        if (WebSocketEventListener.getSessionList().isEmpty()) {
            var user = new User(message.getSender(), User.Role.ADMIN);
            WebSocketEventListener.getSessionList().add(user);
        } else if (WebSocketEventListener.getSessionList().size() == 1) {
            var user = new User(message.getSender(), User.Role.USER);
            WebSocketEventListener.getSessionList().add(user);
        }
        if (WebSocketEventListener.getSessionList().size() < 2) {
            message.setContent("Waiting for another player");
        }
    }
}
