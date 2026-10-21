package com.collabx.collaboration.model;

import java.time.Instant;

public class LanguageChangeDto {

    private String roomId;
    private String language;
    private String fileName;
    private String requestedBy;
    private Instant updatedAt;

    public LanguageChangeDto() {
        this.updatedAt = Instant.now();
    }

    public LanguageChangeDto(String roomId, String language, String fileName, String requestedBy) {
        this.roomId = roomId;
        this.language = language;
        this.fileName = fileName;
        this.requestedBy = requestedBy;
        this.updatedAt = Instant.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
