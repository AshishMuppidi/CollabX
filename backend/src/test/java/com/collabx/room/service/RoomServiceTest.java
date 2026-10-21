package com.collabx.room.service;

import com.collabx.common.exception.BadRequestException;
import com.collabx.common.exception.InvalidPasswordException;
import com.collabx.common.exception.ResourceNotFoundException;
import com.collabx.common.security.RoomTokenService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private PasswordEncoder passwordEncoder;
    private RoomTokenService roomTokenService;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        roomTokenService = new RoomTokenService("01234567890123456789012345678901");
        roomService = new RoomService(roomRepository, passwordEncoder, roomTokenService);
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
        assertNotNull(dto.getRoomToken());
        assertTrue(roomTokenService.isValid(dto.getRoomToken(), dto.getRoomId(), "Alice"));
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
        assertNotNull(dto.getRoomToken());
    }

    @Test
    @DisplayName("Attempt to overwrite existing room with duplicate ID throws BadRequestException")
    void testCreateRoom_DuplicateIdRejected() {
        CreateRoomRequest request = new CreateRoomRequest("EXISTING", "Eve", "hacker123");
        Room existing = new Room("EXISTING", "existing code", "hash", "Alice", "javascript");

        when(roomRepository.findByRoomId("EXISTING")).thenReturn(Optional.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.createRoom(request));
        assertEquals("Room ID already in use", ex.getMessage());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Create room with invalid ID format fails")
    void testCreateRoom_InvalidIdFormat() {
        CreateRoomRequest request = new CreateRoomRequest("!", "Bob", "pass1234");
        assertThrows(BadRequestException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("Create room with short password fails")
    void testCreateRoom_ShortPassword() {
        CreateRoomRequest request = new CreateRoomRequest(null, "Bob", "ab");
        assertThrows(BadRequestException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("Verify room with correct BCrypt password succeeds and returns token")
    void testVerifyRoom_Success() {
        String hash = passwordEncoder.encode("secretPass");
        Room room = new Room("ROOM123", "", hash, "Alice", "javascript");

        when(roomRepository.findByRoomId("ROOM123")).thenReturn(Optional.of(room));

        VerifyRoomResponse response = roomService.verifyRoom(new VerifyRoomRequest("ROOM123", "secretPass", "Charlie"));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("ROOM123", response.getRoomId());
        assertNotNull(response.getRoomToken());
        assertTrue(roomTokenService.isValid(response.getRoomToken(), "ROOM123", "Charlie"));
    }

    @Test
    @DisplayName("Legacy SHA-256 password is authenticated and migrated to BCrypt")
    void testVerifyRoom_LegacySha256MigratedToBcrypt() throws Exception {
        String legacyPlain = "oldLegacyPassword";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(legacyPlain.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        String sha256Hash = sb.toString();

        Room room = new Room("LEGACY1", "", sha256Hash, "Dave", "javascript");
        when(roomRepository.findByRoomId("LEGACY1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyRoomResponse response = roomService.verifyRoom(new VerifyRoomRequest("LEGACY1", legacyPlain, "Dave"));

        assertTrue(response.isSuccess());
        // Verify the room's password was updated to BCrypt
        assertTrue(room.getPasswordHash().startsWith("$2a$") || room.getPasswordHash().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(legacyPlain, room.getPasswordHash()));
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("Plaintext password in database is strictly rejected")
    void testVerifyRoom_PlaintextPasswordRejected() {
        Room room = new Room("PLAIN1", "", "plainPassword123", "Dave", "javascript");
        when(roomRepository.findByRoomId("PLAIN1")).thenReturn(Optional.of(room));

        assertThrows(InvalidPasswordException.class, () ->
                roomService.verifyRoom(new VerifyRoomRequest("PLAIN1", "plainPassword123"))
        );
        verify(roomRepository, never()).save(any(Room.class));
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
