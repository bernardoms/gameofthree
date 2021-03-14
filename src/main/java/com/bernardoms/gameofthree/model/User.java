package com.bernardoms.gameofthree.model;

import lombok.Data;

@Data
public class User {
    private String username;
    private Role role;

    public User(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public enum Role {
        USER,
        ADMIN
    }
}
