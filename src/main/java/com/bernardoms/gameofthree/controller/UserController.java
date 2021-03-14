package com.bernardoms.gameofthree.controller;


import com.bernardoms.gameofthree.event.WebSocketEventListener;
import com.bernardoms.gameofthree.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserController {
    @GetMapping("/user")
    public ResponseEntity<List<User>> getUsersOnSession() {
        return ResponseEntity.ok(WebSocketEventListener.getSessionList());
    }
}
