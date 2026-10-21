package com.collabx.collaboration.model;

import java.time.Instant;

public class ChatMessageDto {

    private String roomId;
    private String username;
    private String message;
    private Instant sentAt;

    public ChatMessageDto() {
        this.sentAt = Instant.now();
    }

    public ChatMessageDto(String roomId, String username, String message) {
        this.roomId = roomId;
        this.username = username;
        this.message = message;
        this.sentAt = Instant.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
