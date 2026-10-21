package com.collabx.collaboration.security;

import com.collabx.collaboration.service.CollaborationService;
import com.collabx.common.security.RoomTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inbound STOMP channel interceptor enforcing room authorization, subscription isolation,
 * payload validation, and user identity protection against impersonation.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private static final Pattern TOPIC_ROOM_PATTERN = Pattern.compile("^/topic/room\\.([^.]+)\\..*");
    private static final Pattern APP_ROOM_PATTERN = Pattern.compile("^/app/room/([^/]+)/(.+)");

    private final RoomTokenService roomTokenService;

    public StompAuthChannelInterceptor(RoomTokenService roomTokenService) {
        this.roomTokenService = roomTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String sessionId = accessor.getSessionId();

        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor, sessionId, sessionAttributes);
            case SEND -> handleSend(message, accessor, sessionId, sessionAttributes);
            default -> {}
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token != null) {
            RoomTokenService.TokenClaims claims = roomTokenService.validateToken(token, null);
            if (claims != null && accessor.getSessionAttributes() != null) {
                bindSession(accessor.getSessionAttributes(), claims.getRoomId(), claims.getUsername());
                log.debug("Pre-authenticated STOMP session {} for room '{}', user '{}'",
                        accessor.getSessionId(), claims.getRoomId(), claims.getUsername());
            }
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor, String sessionId, Map<String, Object> sessionAttributes) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = TOPIC_ROOM_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String targetRoomId = matcher.group(1).toUpperCase();

            // Check if token was provided in SUBSCRIBE headers
            String token = extractToken(accessor);
            if (token != null) {
                RoomTokenService.TokenClaims claims = roomTokenService.validateToken(token, targetRoomId);
                if (claims != null && sessionAttributes != null) {
                    bindSession(sessionAttributes, claims.getRoomId(), claims.getUsername());
                }
            }

            // Verify session authorization for target room
            if (!isSessionAuthorizedForRoom(sessionAttributes, targetRoomId)) {
                log.warn("Subscription blocked: Session {} is NOT authorized for room '{}'", sessionId, targetRoomId);
                throw new AccessDeniedException("Unauthorized subscription to room: " + targetRoomId);
            }
        }
    }

    private void handleSend(Message<?> message, StompHeaderAccessor accessor, String sessionId, Map<String, Object> sessionAttributes) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = APP_ROOM_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        String targetRoomId = matcher.group(1).toUpperCase();
        String action = matcher.group(2).toLowerCase();

        // 1. Validate payload size
        Object payloadObj = message.getPayload();
        if (payloadObj instanceof byte[] bytes && bytes.length > 200_000) {
            log.warn("Message rejected: payload size {} bytes exceeds 200KB limit for session {}", bytes.length, sessionId);
            throw new IllegalArgumentException("Message payload exceeds maximum allowed size (200 KB)");
        }

        // 2. Handle room join
        if ("join".equals(action)) {
            String token = extractToken(accessor);
            if (token == null && payloadObj instanceof byte[] bytes) {
                // Check if token is in JSON payload
                String payloadStr = new String(bytes, StandardCharsets.UTF_8);
                token = extractTokenFromJson(payloadStr);
            }

            if (token == null) {
                log.warn("Join rejected: Missing room access token for room '{}'", targetRoomId);
                throw new AccessDeniedException("Missing room access token for join to room: " + targetRoomId);
            }

            RoomTokenService.TokenClaims claims = roomTokenService.validateToken(token, targetRoomId);
            if (claims == null) {
                log.warn("Join rejected: Invalid or expired room access token for room '{}'", targetRoomId);
                throw new AccessDeniedException("Invalid or expired room access token for room: " + targetRoomId);
            }

            if (sessionAttributes != null) {
                bindSession(sessionAttributes, claims.getRoomId(), claims.getUsername());
            }
            return;
        }

        // 3. Handle all other room actions (code, chat, language, typing, preview, leave)
        if (!isSessionAuthorizedForRoom(sessionAttributes, targetRoomId)) {
            log.warn("Publication rejected: Session {} is not authorized to publish to room '{}'", sessionId, targetRoomId);
            throw new AccessDeniedException("Unauthorized message publication to room: " + targetRoomId);
        }
    }

    private boolean isSessionAuthorizedForRoom(Map<String, Object> sessionAttributes, String roomId) {
        if (sessionAttributes == null || roomId == null) {
            return false;
        }
        String authorizedRoom = (String) sessionAttributes.get("authorized_room");
        if (authorizedRoom != null && authorizedRoom.equalsIgnoreCase(roomId.trim())) {
            return true;
        }
        Boolean isAuth = (Boolean) sessionAttributes.get("authorized_room_" + roomId.trim().toUpperCase());
        return Boolean.TRUE.equals(isAuth);
    }

    private void bindSession(Map<String, Object> sessionAttributes, String roomId, String username) {
        String normalizedRoom = roomId.trim().toUpperCase();
        String normalizedUser = username.trim();
        sessionAttributes.put("authorized_room", normalizedRoom);
        sessionAttributes.put("authorized_user", normalizedUser);
        sessionAttributes.put("authorized_room_" + normalizedRoom, true);
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("X-Room-Token");
        if (token == null || token.trim().isEmpty()) {
            token = accessor.getFirstNativeHeader("roomToken");
        }
        if (token == null || token.trim().isEmpty()) {
            token = accessor.getFirstNativeHeader("token");
        }
        return (token != null && !token.trim().isEmpty()) ? token.trim() : null;
    }

    private String extractTokenFromJson(String json) {
        if (json == null) return null;
        // Fast regex to extract "token" or "roomToken"
        Pattern p = Pattern.compile("\"(?:roomToken|token)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
