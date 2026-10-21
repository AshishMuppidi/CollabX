package com.collabx.room.controller;

import com.collabx.room.dto.CreateRoomRequest;
import com.collabx.room.dto.VerifyRoomRequest;
import com.collabx.room.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomService roomService;

    @Test
    @DisplayName("Health endpoint returns 200 OK")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("Create room via POST /api/rooms returns 201 Created")
    void testCreateRoom_Api() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("TESTROOM1", "Alice", "password123");

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.room.roomId").value("TESTROOM1"))
                .andExpect(jsonPath("$.room.creatorUsername").value("Alice"));
    }

    @Test
    @DisplayName("Create room via legacy POST /rooms returns 201 Created")
    void testCreateRoom_LegacyPath() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("TESTROOM2", "Bob", "secret456");

        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.room.roomId").value("TESTROOM2"));
    }

    @Test
    @DisplayName("Verify room with valid password returns 200 OK")
    void testVerifyRoom_Valid() throws Exception {
        roomService.createRoom(new CreateRoomRequest("TESTVERIFY", "Charlie", "correctPass"));

        VerifyRoomRequest request = new VerifyRoomRequest("TESTVERIFY", "correctPass");

        mockMvc.perform(post("/api/rooms/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.roomId").value("TESTVERIFY"));
    }

    @Test
    @DisplayName("Verify room with wrong password returns 401 Unauthorized")
    void testVerifyRoom_Invalid() throws Exception {
        roomService.createRoom(new CreateRoomRequest("TESTWRONG", "Dave", "correctPass"));

        VerifyRoomRequest request = new VerifyRoomRequest("TESTWRONG", "wrongPass");

        mockMvc.perform(post("/api/rooms/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Verify non-existent room returns 404 Not Found")
    void testVerifyRoom_NotFound() throws Exception {
        VerifyRoomRequest request = new VerifyRoomRequest("NONEXISTENT", "somePass");

        mockMvc.perform(post("/api/rooms/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Attempt to create duplicate room returns 400 Bad Request")
    void testCreateRoom_DuplicateRejected() throws Exception {
        CreateRoomRequest req1 = new CreateRoomRequest("DUPROOM1", "Alice", "pass123");
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomToken").isNotEmpty());

        CreateRoomRequest req2 = new CreateRoomRequest("DUPROOM1", "Bob", "pass456");
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Room ID already in use"));
    }

    @Test
    @DisplayName("Create room with short password returns 400 Bad Request")
    void testCreateRoom_InvalidPassword() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("SHORTPW", "Alice", "ab");
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
