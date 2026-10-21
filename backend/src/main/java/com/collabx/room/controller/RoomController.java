package com.collabx.room.controller;

import com.collabx.common.dto.ApiResponse;
import com.collabx.room.dto.CreateRoomRequest;
import com.collabx.room.dto.RoomDto;
import com.collabx.room.dto.VerifyRoomRequest;
import com.collabx.room.dto.VerifyRoomResponse;
import com.collabx.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping({"/api/rooms", "/rooms"})
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDto roomDto = roomService.createRoom(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Room created successfully");
        response.put("room", roomDto);
        response.put("data", roomDto);
        response.put("roomId", roomDto.getRoomId());
        response.put("roomToken", roomDto.getRoomToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/api/rooms/verify", "/rooms/verify"})
    public ResponseEntity<VerifyRoomResponse> verifyRoom(@Valid @RequestBody VerifyRoomRequest request) {
        VerifyRoomResponse response = roomService.verifyRoom(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/rooms/{roomId}", "/rooms/{roomId}"})
    public ResponseEntity<ApiResponse<RoomDto>> getRoom(@PathVariable String roomId) {
        RoomDto roomDto = roomService.getRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room retrieved successfully", roomDto));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        res.put("service", "CollabX Backend (Spring Boot 3.3.4, Java 17)");
        res.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(res);
    }
}
