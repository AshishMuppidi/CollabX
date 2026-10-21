package com.collabx.codeexecution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ExecutionResultResponse {

    private boolean success;
    private String status;
    private String output;
    private String stdout;
    private String stderr;

    @JsonProperty("compileOutput")
    private String compileOutput;

    @JsonProperty("compileError")
    private String compileError;

    private String time;
    private String memory;
    private String message;
    private String preview;
    private String runner;
    private String roomId;
    private String language;
    private Instant timestamp;

    public ExecutionResultResponse() {
        this.timestamp = Instant.now();
    }

    public static ExecutionResultResponse success(String stdout, String time, String memory, String runner) {
        ExecutionResultResponse res = new ExecutionResultResponse();
        res.setSuccess(true);
        res.setStatus("Completed");
        res.setStdout(stdout);
        res.setOutput(stdout != null && !stdout.trim().isEmpty() ? stdout : "Execution completed with no output.");
        res.setTime(time);
        res.setMemory(memory);
        res.setRunner(runner);
        return res;
    }

    public static ExecutionResultResponse htmlPreview(String html) {
        ExecutionResultResponse res = new ExecutionResultResponse();
        res.setSuccess(true);
        res.setStatus("Completed");
        res.setOutput("HTML preview rendered successfully.");
        res.setPreview(html);
        res.setRunner("browser-preview");
        return res;
    }

    public static ExecutionResultResponse error(String status, String stderr, String compileOutput, String message) {
        ExecutionResultResponse res = new ExecutionResultResponse();
        res.setSuccess(false);
        res.setStatus(status != null ? status : "Error");
        res.setStderr(stderr);
        res.setCompileOutput(compileOutput);
        res.setCompileError(compileOutput);
        res.setMessage(message);

        StringBuilder sb = new StringBuilder();
        if (compileOutput != null && !compileOutput.trim().isEmpty()) {
            sb.append("Compile Output:\n").append(compileOutput.trim()).append("\n");
        }
        if (stderr != null && !stderr.trim().isEmpty()) {
            sb.append("Error:\n").append(stderr.trim()).append("\n");
        }
        if (message != null && !message.trim().isEmpty()) {
            sb.append("Message: ").append(message.trim()).append("\n");
        }
        res.setOutput(sb.length() > 0 ? sb.toString().trim() : "Execution failed.");
        res.setRunner("judge0");
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getCompileOutput() {
        return compileOutput;
    }

    public void setCompileOutput(String compileOutput) {
        this.compileOutput = compileOutput;
        this.compileError = compileOutput;
    }

    public String getCompileError() {
        return compileError;
    }

    public void setCompileError(String compileError) {
        this.compileError = compileError;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getRunner() {
        return runner;
    }

    public void setRunner(String runner) {
        this.runner = runner;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
