package com.collabx.room.service;

import com.collabx.common.exception.BadRequestException;
import com.collabx.common.exception.InvalidPasswordException;
import com.collabx.common.exception.ResourceNotFoundException;
import com.collabx.room.dto.CreateRoomRequest;
import com.collabx.room.dto.RoomDto;
import com.collabx.room.dto.VerifyRoomRequest;
import com.collabx.room.dto.VerifyRoomResponse;
import com.collabx.room.entity.Room;
import com.collabx.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private PasswordEncoder passwordEncoder;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        roomService = new RoomService(roomRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Create room with generated ID")
    void testCreateRoom_GeneratedId() {
        CreateRoomRequest request = new CreateRoomRequest(null, "Alice", "secret123");

        when(roomRepository.findByRoomId(any())).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomDto dto = roomService.createRoom(request);

        assertNotNull(dto);
        assertNotNull(dto.getRoomId());
        assertEquals(8, dto.getRoomId().length());
        assertEquals("Alice", dto.getCreatorUsername());
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    @DisplayName("Create room with custom valid ID")
    void testCreateRoom_CustomId() {
        CreateRoomRequest request = new CreateRoomRequest("MYROOM1", "Bob", "pass1234");

        when(roomRepository.findByRoomId("MYROOM1")).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomDto dto = roomService.createRoom(request);

        assertNotNull(dto);
        assertEquals("MYROOM1", dto.getRoomId());
        assertEquals("Bob", dto.getCreatorUsername());
    }

    @Test
    @DisplayName("Create room with invalid ID format fails")
    void testCreateRoom_InvalidIdFormat() {
        CreateRoomRequest request = new CreateRoomRequest("!", "Bob", "pass1234");
        assertThrows(BadRequestException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("Verify room with correct password succeeds")
    void testVerifyRoom_Success() {
        String hash = passwordEncoder.encode("secretPass");
        Room room = new Room("ROOM123", "", hash, "Alice", "javascript");

        when(roomRepository.findByRoomId("ROOM123")).thenReturn(Optional.of(room));

        VerifyRoomResponse response = roomService.verifyRoom(new VerifyRoomRequest("ROOM123", "secretPass"));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("ROOM123", response.getRoomId());
    }

    @Test
    @DisplayName("Verify room with incorrect password throws InvalidPasswordException")
    void testVerifyRoom_WrongPassword() {
        String hash = passwordEncoder.encode("secretPass");
        Room room = new Room("ROOM123", "", hash, "Alice", "javascript");

        when(roomRepository.findByRoomId("ROOM123")).thenReturn(Optional.of(room));

        assertThrows(InvalidPasswordException.class, () ->
                roomService.verifyRoom(new VerifyRoomRequest("ROOM123", "wrongPass"))
        );
    }

    @Test
    @DisplayName("Verify non-existent room throws ResourceNotFoundException")
    void testVerifyRoom_NotFound() {
        when(roomRepository.findByRoomId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                roomService.verifyRoom(new VerifyRoomRequest("UNKNOWN", "secretPass"))
        );
    }
}
