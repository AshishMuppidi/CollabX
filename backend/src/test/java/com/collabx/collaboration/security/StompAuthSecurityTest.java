package com.collabx.collaboration.security;

import com.collabx.collaboration.service.CollaborationService;
import com.collabx.common.security.RoomTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StompAuthSecurityTest {

    @Mock
    private MessageChannel messageChannel;

    private RoomTokenService roomTokenService;
    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        roomTokenService = new RoomTokenService("01234567890123456789012345678901");
        interceptor = new StompAuthChannelInterceptor(roomTokenService);
    }

    @Test
    @DisplayName("Subscription to room without token or prior authorization throws AccessDeniedException")
    void testUnauthorizedSubscriptionRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room.ROOM_B.code");
        accessor.setSessionId("sess-unauth");
        accessor.setSessionAttributes(new HashMap<>());

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Participant authorized for Room A is blocked from subscribing to Room B")
    void testCrossRoomSubscriptionBlocked() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room.ROOM_B.chat");
        accessor.setSessionId("sess-roomA");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("authorized_room", "ROOM_A");
        sessionAttrs.put("authorized_user", "Alice");
        accessor.setSessionAttributes(sessionAttrs);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Participant authorized for Room A is blocked from publishing to Room B")
    void testCrossRoomPublishBlocked() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/ROOM_B/chat");
        accessor.setSessionId("sess-roomA");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("authorized_room", "ROOM_A");
        sessionAttrs.put("authorized_user", "Alice");
        accessor.setSessionAttributes(sessionAttrs);

        byte[] payload = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Join with valid token grants session authorization for target room")
    void testJoinWithValidTokenSuccess() {
        String token = roomTokenService.generateToken("ROOM_A", "Alice");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/ROOM_A/join");
        accessor.setSessionId("sess-join");
        accessor.setNativeHeader("X-Room-Token", token);
        Map<String, Object> sessionAttrs = new HashMap<>();
        accessor.setSessionAttributes(sessionAttrs);

        byte[] payload = "{\"username\":\"Alice\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        assertEquals("ROOM_A", sessionAttrs.get("authorized_room"));
        assertEquals("Alice", sessionAttrs.get("authorized_user"));
        assertEquals(Boolean.TRUE, sessionAttrs.get("authorized_room_ROOM_A"));
    }

    @Test
    @DisplayName("Join with invalid token throws AccessDeniedException")
    void testJoinWithInvalidTokenRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/ROOM_A/join");
        accessor.setSessionId("sess-bad");
        accessor.setNativeHeader("X-Room-Token", "invalid-fake-token");
        accessor.setSessionAttributes(new HashMap<>());

        byte[] payload = "{\"username\":\"Eve\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Join without any token throws AccessDeniedException")
    void testJoinWithoutTokenRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/ROOM_A/join");
        accessor.setSessionId("sess-notoken");
        accessor.setSessionAttributes(new HashMap<>());

        byte[] payload = "{\"username\":\"Eve\"}".getBytes(StandardCharsets.UTF_8);
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Oversized payload throws IllegalArgumentException")
    void testOversizedPayloadRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/ROOM_A/code");
        accessor.setSessionId("sess-large");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("authorized_room", "ROOM_A");
        accessor.setSessionAttributes(sessionAttrs);

        byte[] largePayload = new byte[250_000]; // 250 KB
        Message<byte[]> message = MessageBuilder.createMessage(largePayload, accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, messageChannel));
    }
}
