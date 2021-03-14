package com.bernardoms.gameofthree.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Message {
    private String content;
    private String sender;
    private MessageType type;
    private String to;

    @JsonIgnore
    public boolean isValidMessage() {
        return this.getContent().equals("-1") || this.getContent().equals("0") || this.getContent().equals("1");
    }

    public enum MessageType {
        START,
        JOIN,
        LEAVE,
        PLAY,
        ERROR,
        WON
    }
}
