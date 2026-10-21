package com.collabx.room.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class RoomDto {

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("room_id")
    private String roomIdSnake;

    @JsonProperty("creatorUsername")
    private String creatorUsername;

    @JsonProperty("creator_username")
    private String creatorUsernameSnake;

    private String code;
    private String currentLanguage;
    private Instant createdAt;

    public RoomDto() {
    }

    public RoomDto(String roomId, String creatorUsername, String code, String currentLanguage, Instant createdAt) {
        this.roomId = roomId;
        this.roomIdSnake = roomId;
        this.creatorUsername = creatorUsername;
        this.creatorUsernameSnake = creatorUsername;
        this.code = code;
        this.currentLanguage = currentLanguage;
        this.createdAt = createdAt;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        this.roomIdSnake = roomId;
    }

    public String getRoomIdSnake() {
        return roomIdSnake;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
        this.creatorUsernameSnake = creatorUsername;
    }

    public String getCreatorUsernameSnake() {
        return creatorUsernameSnake;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setCurrentLanguage(String currentLanguage) {
        this.currentLanguage = currentLanguage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
