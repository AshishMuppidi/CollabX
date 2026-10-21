package com.collabx.collaboration.model;

public class TypingDto {

    private String roomId;
    private String username;

    public TypingDto() {
    }

    public TypingDto(String roomId, String username) {
        this.roomId = roomId;
        this.username = username;
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
}
