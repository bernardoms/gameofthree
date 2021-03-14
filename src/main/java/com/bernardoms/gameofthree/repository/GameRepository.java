package com.bernardoms.gameofthree.repository;


import com.bernardoms.gameofthree.exception.GameException;
import com.bernardoms.gameofthree.model.Game;

import java.util.List;

public interface GameRepository {
    List<Game> getGame();
    void saveGame(Game game);
    void deleteGame(Game game);
    Game updateGame(Game game) throws GameException;
}
