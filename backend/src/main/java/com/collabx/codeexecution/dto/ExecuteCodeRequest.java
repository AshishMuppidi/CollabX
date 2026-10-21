package com.collabx.codeexecution.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class ExecuteCodeRequest {

    private String roomId;

    @NotBlank(message = "Code is required")
    @JsonAlias({"sourceCode", "script", "content"})
    private String code;

    @NotBlank(message = "Language is required")
    private String language;

    private Integer languageId;

    private String stdin = "";

    private String username = "Anonymous";

    public ExecuteCodeRequest() {
    }

    public ExecuteCodeRequest(String roomId, String code, String language, Integer languageId, String stdin, String username) {
        this.roomId = roomId;
        this.code = code;
        this.language = language;
        this.languageId = languageId;
        this.stdin = stdin;
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
}
