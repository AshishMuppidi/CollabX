package com.collabx.collaboration.model;

public class CodeSyncDto {

    private String roomId;
    private String code;
    private String username;

    public CodeSyncDto() {
    }

    public CodeSyncDto(String roomId, String code, String username) {
        this.roomId = roomId;
        this.code = code;
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
