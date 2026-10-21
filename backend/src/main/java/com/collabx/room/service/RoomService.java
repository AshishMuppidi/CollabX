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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    private static final String ROOM_ID_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int ROOM_ID_LENGTH = 8;
    private final SecureRandom secureRandom = new SecureRandom();

    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    public RoomService(RoomRepository roomRepository, PasswordEncoder passwordEncoder) {
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        String roomId = request.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            roomId = generateUniqueRoomId();
        } else {
            roomId = roomId.trim().toUpperCase();
            if (!roomId.matches("^[A-Za-z0-9_-]{3,64}$")) {
                throw new BadRequestException("Room ID must be 3-64 alphanumeric characters");
            }
        }

        String username = request.getUsername().trim();
        String password = request.getPassword().trim();

        if (password.length() < 3) {
            throw new BadRequestException("Password must be at least 3 characters");
        }

        String passwordHash = passwordEncoder.encode(password);

        Optional<Room> existing = roomRepository.findByRoomId(roomId);
        if (existing.isPresent()) {
            // If a room with the requested ID already exists, reject request.
            throw new BadRequestException("Room ID already in use");
        }
        // Create a new room.
        Room room = new Room(roomId, "", passwordHash, username, "javascript");

        Room saved = roomRepository.save(room);
        log.info("Room '{}' created/updated by user '{}'", roomId, username);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public VerifyRoomResponse verifyRoom(VerifyRoomRequest request) {
        String roomId = normalizeRoomId(request.getRoomId());
        String password = request.getPassword().trim();

        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!checkPassword(password, room.getPasswordHash())) {
            throw new InvalidPasswordException("Invalid room password");
        }

        return VerifyRoomResponse.ok(roomId);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoom(String roomId) {
        String normalizedId = normalizeRoomId(roomId);
        Room room = roomRepository.findByRoomId(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalizedId));
        return toDto(room);
    }

    @Transactional
    public void saveCode(String roomId, String code) {
        String normalizedId = normalizeRoomId(roomId);
        roomRepository.updateRoomCode(normalizedId, code != null ? code : "", Instant.now());
    }

    @Transactional
    public void updateLanguage(String roomId, String language) {
        String normalizedId = normalizeRoomId(roomId);
        roomRepository.updateRoomLanguage(normalizedId, language != null ? language : "javascript", Instant.now());
    }

    public String generateUniqueRoomId() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
            for (int j = 0; j < ROOM_ID_LENGTH; j++) {
                int index = secureRandom.nextInt(ROOM_ID_CHARACTERS.length());
                sb.append(ROOM_ID_CHARACTERS.charAt(index));
            }
            String candidate = sb.toString();
            if (roomRepository.findByRoomId(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "ROOM" + System.currentTimeMillis();
    }

    private boolean checkPassword(String plainPassword, String storedHash) {
        if (storedHash == null || plainPassword == null) {
            return false;
        }

        // Standard BCrypt check
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return passwordEncoder.matches(plainPassword, storedHash);
        }

        // Backward compatibility: SHA-256 hash check
        if (storedHash.length() == 64 && storedHash.matches("^[a-fA-F0-9]{64}$")) {
            return sha256(plainPassword).equalsIgnoreCase(storedHash);
        }

        // Backward compatibility: Plaintext match
        return storedHash.equals(plainPassword);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String normalizeRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new BadRequestException("Room ID cannot be empty");
        }
        return roomId.trim().toUpperCase();
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(
                room.getRoomId(),
                room.getCreatorUsername(),
                room.getCode(),
                room.getCurrentLanguage(),
                room.getCreatedAt()
        );
    }
}
