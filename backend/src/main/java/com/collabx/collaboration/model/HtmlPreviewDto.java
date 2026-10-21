package com.collabx.collaboration.model;

import java.time.Instant;

public class HtmlPreviewDto {

    private String roomId;
    private String html;
    private String username;
    private Instant updatedAt;

    public HtmlPreviewDto() {
        this.updatedAt = Instant.now();
    }

    public HtmlPreviewDto(String roomId, String html, String username) {
        this.roomId = roomId;
        this.html = html;
        this.username = username;
        this.updatedAt = Instant.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
