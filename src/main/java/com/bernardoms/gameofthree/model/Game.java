package com.bernardoms.gameofthree.model;

import lombok.Data;

import java.util.List;

@Data
public class Game {
    private List<User> users;
    private boolean hasGameStarted;
    private Integer generatedNumber;
    private String playerTurn;

    public int sumToGeneratedNumber(int num) {
        return this.generatedNumber + num;
    }
}
