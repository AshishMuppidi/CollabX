package com.collabx.room.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyRoomRequest {

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotBlank(message = "Password is required")
    private String password;

    private String username;

    public VerifyRoomRequest() {
    }

    public VerifyRoomRequest(String roomId, String password) {
        this(roomId, password, null);
    }

    public VerifyRoomRequest(String roomId, String password, String username) {
        this.roomId = roomId;
        this.password = password;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
