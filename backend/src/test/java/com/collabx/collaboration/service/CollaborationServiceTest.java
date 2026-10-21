package com.collabx.collaboration.service;

import com.collabx.collaboration.model.ChatMessageDto;
import com.collabx.collaboration.model.CodeSyncDto;
import com.collabx.collaboration.model.LanguageChangeDto;
import com.collabx.collaboration.model.UserPresenceDto;
import com.collabx.room.dto.RoomDto;
import com.collabx.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomService roomService;

    private CollaborationService collaborationService;

    @BeforeEach
    void setUp() {
        collaborationService = new CollaborationService(messagingTemplate, roomService);
    }

    @Test
    @DisplayName("User join tracks presence and broadcasts")
    void testUserJoined() {
        when(roomService.getRoom("ROOM1")).thenReturn(new RoomDto("ROOM1", "Alice", "// code", "javascript", Instant.now()));

        collaborationService.userJoined("session-1", "ROOM1", "Alice");

        List<UserPresenceDto> users = collaborationService.getOnlineUsers("ROOM1");
        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getUsername());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/room.ROOM1.users"), any(List.class));
    }

    @Test
    @DisplayName("User reconnection with same username deduplicates presence")
    void testUserReconnectionDeduplicated() {
        when(roomService.getRoom("ROOM1")).thenReturn(new RoomDto("ROOM1", "Alice", "// code", "javascript", Instant.now()));

        // First connection
        collaborationService.userJoined("session-old", "ROOM1", "Alice");
        // Reconnect with new session
        collaborationService.userJoined("session-new", "ROOM1", "Alice");

        List<UserPresenceDto> users = collaborationService.getOnlineUsers("ROOM1");
        assertEquals(1, users.size(), "Should have exactly 1 presence entry for Alice, not duplicate");
        assertEquals("session-new", users.get(0).getId());
    }

    @Test
    @DisplayName("User left updates presence")
    void testUserLeft() {
        when(roomService.getRoom("ROOM1")).thenReturn(new RoomDto("ROOM1", "Alice", "// code", "javascript", Instant.now()));

        collaborationService.userJoined("session-1", "ROOM1", "Alice");
        collaborationService.userLeft("session-1");

        List<UserPresenceDto> users = collaborationService.getOnlineUsers("ROOM1");
        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("Code sync updates persistence and broadcasts to topic")
    void testBroadcastCode() {
        CodeSyncDto dto = new CodeSyncDto("ROOM1", "console.log('test')", "Alice");

        collaborationService.broadcastCode("ROOM1", dto);

        verify(roomService, times(1)).saveCode("ROOM1", "console.log('test')");
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room.ROOM1.code", dto);
    }

    @Test
    @DisplayName("Chat message broadcasts to room chat topic")
    void testBroadcastChat() {
        ChatMessageDto msg = new ChatMessageDto("ROOM1", "Alice", "Hello everyone!");

        collaborationService.broadcastChat("ROOM1", msg);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/room.ROOM1.chat", msg);
    }

    @Test
    @DisplayName("Language change updates room and broadcasts")
    void testSetRoomLanguage() {
        LanguageChangeDto dto = new LanguageChangeDto("ROOM1", "python", "main.py", "Bob");

        collaborationService.setRoomLanguage("ROOM1", dto);

        verify(roomService, times(1)).updateLanguage("ROOM1", "python");
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room.ROOM1.language", dto);
    }
}
