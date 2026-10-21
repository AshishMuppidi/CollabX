package com.collabx.room.dto;

public class VerifyRoomResponse {

    private boolean success;
    private String message;
    private String roomId;

    public VerifyRoomResponse() {
    }

    public VerifyRoomResponse(boolean success, String message, String roomId) {
        this.success = success;
        this.message = message;
        this.roomId = roomId;
    }

    public static VerifyRoomResponse ok(String roomId) {
        return new VerifyRoomResponse(true, "Room verified successfully", roomId);
    }

    public static VerifyRoomResponse failed(String message) {
        return new VerifyRoomResponse(false, message, null);
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
