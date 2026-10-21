package com.collabx.collaboration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class UserPresenceDto {

    private String id;
    private String username;

    @JsonProperty("socketId")
    private String socketId;

    public UserPresenceDto() {
    }

    public UserPresenceDto(String id, String username) {
        this.id = id;
        this.username = username;
        this.socketId = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.socketId = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPresenceDto that = (UserPresenceDto) o;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
