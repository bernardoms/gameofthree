package com.bernardoms.gameofthree.repository;

import com.bernardoms.gameofthree.exception.GameException;
import com.bernardoms.gameofthree.model.Game;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("GameRepository")
public class GameRepositoryImpl implements GameRepository {
    private final List<Game> games = new ArrayList<>();

    @Override
    public List<Game> getGame() {
        return this.games;
    }

    @Override
    public void saveGame(Game game) {
        this.games.add(game);
    }

    @Override
    public void deleteGame(Game game) {
        this.games.remove(game);
    }

    @Override
    public Game updateGame(Game game) throws GameException {
        return games.stream().filter(g -> g.equals(game)).findFirst().orElseThrow(() -> new GameException("Game in progress not found!"));
    }
}
