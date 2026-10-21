package com.collabx.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @Column(name = "room_id", length = 64, nullable = false, updatable = false)
    private String roomId;

    @Column(name = "code", columnDefinition = "LONGTEXT")
    private String code;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "creator_username", length = 100, nullable = false)
    private String creatorUsername;

    @Column(name = "current_language", length = 50)
    private String currentLanguage = "javascript";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Room() {
    }

    public Room(String roomId, String code, String passwordHash, String creatorUsername, String currentLanguage) {
        this.roomId = roomId;
        this.code = code;
        this.passwordHash = passwordHash;
        this.creatorUsername = creatorUsername;
        this.currentLanguage = currentLanguage != null ? currentLanguage : "javascript";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
