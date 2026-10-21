package com.collabx.collaboration.service;

import com.collabx.collaboration.model.*;
import com.collabx.room.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollaborationService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    // sessionId -> roomId
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    // sessionId -> username
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    // roomId -> Set of users
    private final Map<String, Set<UserPresenceDto>> roomUsersMap = new ConcurrentHashMap<>();
    // roomId -> active language state
    private final Map<String, LanguageChangeDto> roomLanguageMap = new ConcurrentHashMap<>();

    public CollaborationService(SimpMessagingTemplate messagingTemplate, RoomService roomService) {
        this.messagingTemplate = messagingTemplate;
        this.roomService = roomService;
    }

    public synchronized void userJoined(String sessionId, String roomId, String username) {
        if (roomId == null || username == null || sessionId == null) {
            return;
        }

        String normalizedRoomId = roomId.trim().toUpperCase();
        String normalizedUsername = username.trim();

        sessionRoomMap.put(sessionId, normalizedRoomId);
        sessionUserMap.put(sessionId, normalizedUsername);

        roomUsersMap.computeIfAbsent(normalizedRoomId, k -> ConcurrentHashMap.newKeySet())
                .add(new UserPresenceDto(sessionId, normalizedUsername));

        log.info("User '{}' (session: {}) joined room '{}'", normalizedUsername, sessionId, normalizedRoomId);

        broadcastPresence(normalizedRoomId);

        // Send current room language
        LanguageChangeDto currentLang = getRoomLanguage(normalizedRoomId);
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".language", currentLang);

        // Send saved code to user
        try {
            var room = roomService.getRoom(normalizedRoomId);
            if (room.getCode() != null && !room.getCode().isEmpty()) {
                messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".code",
                        new CodeSyncDto(normalizedRoomId, room.getCode(), "SYSTEM"));
            }
        } catch (Exception e) {
            log.warn("Could not load initial saved code for room: {}", normalizedRoomId);
        }
    }

    public synchronized void userLeft(String sessionId) {
        String roomId = sessionRoomMap.remove(sessionId);
        String username = sessionUserMap.remove(sessionId);

        if (roomId != null) {
            Set<UserPresenceDto> users = roomUsersMap.get(roomId);
            if (users != null) {
                users.removeIf(u -> u.getId().equals(sessionId));
                if (users.isEmpty()) {
                    roomUsersMap.remove(roomId);
                }
            }

            log.info("User '{}' (session: {}) left room '{}'", username, sessionId, roomId);
            broadcastPresence(roomId);
        }
    }

    public List<UserPresenceDto> getOnlineUsers(String roomId) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        Set<UserPresenceDto> users = roomUsersMap.get(normalizedRoomId);
        if (users == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(users);
    }

    public void broadcastPresence(String roomId) {
        List<UserPresenceDto> users = getOnlineUsers(roomId);
        // Broadcast on STOMP topic for this room
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".users", users);
    }

    public LanguageChangeDto getRoomLanguage(String roomId) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        return roomLanguageMap.computeIfAbsent(normalizedRoomId, k -> {
            try {
                var room = roomService.getRoom(k);
                String lang = room.getCurrentLanguage() != null ? room.getCurrentLanguage() : "javascript";
                return new LanguageChangeDto(k, lang, getFileNameForLanguage(lang), "system");
            } catch (Exception e) {
                return new LanguageChangeDto(k, "javascript", "main.js", "system");
            }
        });
    }

    public void setRoomLanguage(String roomId, LanguageChangeDto dto) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        roomLanguageMap.put(normalizedRoomId, dto);
        roomService.updateLanguage(normalizedRoomId, dto.getLanguage());
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".language", dto);
    }

    public void broadcastCode(String roomId, CodeSyncDto dto) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        // Persist periodically or on change
        roomService.saveCode(normalizedRoomId, dto.getCode());
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".code", dto);
    }

    public void broadcastChat(String roomId, ChatMessageDto message) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".chat", message);
    }

    public void broadcastTyping(String roomId, TypingDto typingDto) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".typing", typingDto);
    }

    public void broadcastPreview(String roomId, HtmlPreviewDto previewDto) {
        String normalizedRoomId = roomId.trim().toUpperCase();
        messagingTemplate.convertAndSend("/topic/room." + normalizedRoomId + ".preview", previewDto);
    }

    public static String getFileNameForLanguage(String language) {
        if (language == null) return "main.js";
        return switch (language.toLowerCase()) {
            case "html" -> "index.html";
            case "javascript", "js", "nodejs" -> "main.js";
            case "typescript", "ts" -> "main.ts";
            case "python", "py", "python3" -> "main.py";
            case "java" -> "Main.java";
            case "c" -> "main.c";
            case "cpp", "c++" -> "main.cpp";
            case "sql", "postgresql" -> "query.sql";
            default -> "main.txt";
        };
    }
}
