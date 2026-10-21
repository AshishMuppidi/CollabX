package com.collabx.codeexecution.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExecuteCodeRequest {

    @Size(max = 64, message = "Room ID must not exceed 64 characters")
    private String roomId;

    @NotBlank(message = "Code is required")
    @Size(max = 65536, message = "Code must not exceed 64 KB")
    @JsonAlias({"sourceCode", "script", "content"})
    private String code;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    private Integer languageId;

    @Size(max = 10000, message = "Stdin must not exceed 10 KB")
    private String stdin = "";

    @Size(max = 100, message = "Username must not exceed 100 characters")
    private String username = "Anonymous";

    private String token;

    public ExecuteCodeRequest() {
    }

    public ExecuteCodeRequest(String roomId, String code, String language, Integer languageId, String stdin, String username) {
        this(roomId, code, language, languageId, stdin, username, null);
    }

    public ExecuteCodeRequest(String roomId, String code, String language, Integer languageId, String stdin, String username, String token) {
        this.roomId = roomId;
        this.code = code;
        this.language = language;
        this.languageId = languageId;
        this.stdin = stdin;
        this.username = username;
        this.token = token;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
