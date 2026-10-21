package com.collabx.codeexecution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Judge0SubmissionRequest {

    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("stdin")
    private String stdin;

    public Judge0SubmissionRequest() {
    }

    public Judge0SubmissionRequest(String sourceCode, int languageId, String stdin) {
        this.sourceCode = sourceCode;
        this.languageId = languageId;
        this.stdin = stdin;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }
}
