package com.collabx.room.dto;

public class VerifyRoomResponse {

    private boolean success;
    private String message;
    private String roomId;
    private String roomToken;

    public VerifyRoomResponse() {
    }

    public VerifyRoomResponse(boolean success, String message, String roomId) {
        this(success, message, roomId, null);
    }

    public VerifyRoomResponse(boolean success, String message, String roomId, String roomToken) {
        this.success = success;
        this.message = message;
        this.roomId = roomId;
        this.roomToken = roomToken;
    }

    public static VerifyRoomResponse ok(String roomId) {
        return new VerifyRoomResponse(true, "Room verified successfully", roomId, null);
    }

    public static VerifyRoomResponse ok(String roomId, String roomToken) {
        return new VerifyRoomResponse(true, "Room verified successfully", roomId, roomToken);
    }

    public static VerifyRoomResponse failed(String message) {
        return new VerifyRoomResponse(false, message, null, null);
    }

    public String getRoomToken() {
        return roomToken;
    }

    public void setRoomToken(String roomToken) {
        this.roomToken = roomToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
