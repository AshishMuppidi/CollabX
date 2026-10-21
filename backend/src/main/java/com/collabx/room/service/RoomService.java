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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomTokenService roomTokenService;

    public RoomService(RoomRepository roomRepository,
                       PasswordEncoder passwordEncoder,
                       RoomTokenService roomTokenService) {
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomTokenService = roomTokenService;
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        if (username.isEmpty() || username.length() > 50) {
            throw new BadRequestException("Username must be between 1 and 50 characters");
        }

        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        if (password.length() < 3 || password.length() > 100) {
            throw new BadRequestException("Password must be between 3 and 100 characters");
        }

        String roomId = request.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            roomId = generateUniqueRoomId();
        } else {
            roomId = roomId.trim().toUpperCase();
            if (!roomId.matches("^[A-Za-z0-9_-]{3,64}$")) {
                throw new BadRequestException("Room ID must be 3-64 alphanumeric characters");
            }
            // Reject client-provided room ID if already taken - never overwrite existing room
            if (roomRepository.findByRoomId(roomId).isPresent()) {
                throw new BadRequestException("Room ID already in use");
            }
        }

        String passwordHash = passwordEncoder.encode(password);
        Room room = new Room(roomId, "", passwordHash, username, "javascript");
        Room saved = roomRepository.save(room);
        log.info("Room '{}' created successfully by user '{}'", roomId, username);

        String roomToken = roomTokenService.generateToken(roomId, username);
        return toDto(saved, roomToken);
    }

    @Transactional
    public VerifyRoomResponse verifyRoom(VerifyRoomRequest request) {
        String roomId = normalizeRoomId(request.getRoomId());
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!checkPasswordAndMigrateIfLegacy(room, password)) {
            throw new InvalidPasswordException("Invalid room password");
        }

        String participantUsername = (request.getUsername() != null && !request.getUsername().trim().isEmpty())
                ? request.getUsername().trim()
                : room.getCreatorUsername();

        String roomToken = roomTokenService.generateToken(roomId, participantUsername);
        return VerifyRoomResponse.ok(roomId, roomToken);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoom(String roomId) {
        String normalizedId = normalizeRoomId(roomId);
        Room room = roomRepository.findByRoomId(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalizedId));
        return toDto(room, null);
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
        for (int i = 0; i < 20; i++) {
            String candidate = roomTokenService.generateSecureRoomId();
            if (roomRepository.findByRoomId(candidate).isEmpty()) {
                return candidate;
            }
        }
        // Fallback to high-entropy candidate
        return roomTokenService.generateSecureRoomId() + roomTokenService.generateSecureRoomId().substring(0, 4);
    }

    private boolean checkPasswordAndMigrateIfLegacy(Room room, String plainPassword) {
        String storedHash = room.getPasswordHash();
        if (storedHash == null || plainPassword == null || plainPassword.isEmpty()) {
            return false;
        }

        // 1. Standard BCrypt verification
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return passwordEncoder.matches(plainPassword, storedHash);
        }

        // 2. Legacy SHA-256 hash verification with transparent upgrade to BCrypt
        if (storedHash.length() == 64 && storedHash.matches("^[a-fA-F0-9]{64}$")) {
            if (sha256(plainPassword).equalsIgnoreCase(storedHash)) {
                log.info("Migrating legacy SHA-256 password to BCrypt for room '{}'", room.getRoomId());
                room.setPasswordHash(passwordEncoder.encode(plainPassword));
                roomRepository.save(room);
                return true;
            }
            return false;
        }

        // Plaintext passwords are strictly rejected for security
        log.warn("Rejected non-BCrypt/insecure password format attempt for room '{}'", room.getRoomId());
        return false;
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

    private RoomDto toDto(Room room, String roomToken) {
        return new RoomDto(
                room.getRoomId(),
                room.getCreatorUsername(),
                room.getCode(),
                room.getCurrentLanguage(),
                room.getCreatedAt(),
                roomToken
        );
    }
}
