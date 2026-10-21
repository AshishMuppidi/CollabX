package com.collabx.room.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyRoomRequest {

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotBlank(message = "Password is required")
    private String password;

    public VerifyRoomRequest() {
    }

    public VerifyRoomRequest(String roomId, String password) {
        this.roomId = roomId;
        this.password = password;
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
