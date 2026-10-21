package com.collabx.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Cryptographic token service for room authorization and secure room ID generation.
 * Issues and validates tamper-proof HMAC-SHA256 tokens scoped to (roomId, username).
 */
@Service
public class RoomTokenService {

    private static final Logger log = LoggerFactory.getLogger(RoomTokenService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ROOM_ID_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int ROOM_ID_LENGTH = 8;
    private static final long DEFAULT_TOKEN_VALIDITY_SECONDS = 86400; // 24 hours

    private final byte[] secretKeyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomTokenService(@Value("${collabx.security.token-secret:}") String configuredSecret) {
        if (configuredSecret != null && configuredSecret.trim().length() >= 32) {
            this.secretKeyBytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
            log.info("Initialized RoomTokenService with configured secret");
        } else {
            byte[] randomKey = new byte[32];
            new SecureRandom().nextBytes(randomKey);
            this.secretKeyBytes = randomKey;
            log.info("Initialized RoomTokenService with auto-generated secure random secret");
        }
    }

    /**
     * Generates a cryptographically secure, URL-safe room ID with high entropy.
     */
    public String generateSecureRoomId() {
        StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            int index = secureRandom.nextInt(ROOM_ID_CHARACTERS.length());
            sb.append(ROOM_ID_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Generates an HMAC-SHA256 signed access token for a specific room and user.
     * Payload: roomId:username:expiresEpochSec:nonce
     */
    public String generateToken(String roomId, String username) {
        if (roomId == null || username == null) {
            throw new IllegalArgumentException("roomId and username must not be null");
        }
        String normalizedRoom = roomId.trim().toUpperCase();
        String normalizedUser = username.trim();
        long expiresAt = Instant.now().getEpochSecond() + DEFAULT_TOKEN_VALIDITY_SECONDS;
        byte[] nonceBytes = new byte[8];
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        String payload = normalizedRoom + ":" + normalizedUser + ":" + expiresAt + ":" + nonce;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = computeHmac(encodedPayload);

        return encodedPayload + "." + signature;
    }

    /**
     * Validates a token and returns the parsed claims if valid and not expired.
     */
    public TokenClaims validateToken(String token, String expectedRoomId) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 2) {
            return null;
        }

        String encodedPayload = parts[0];
        String expectedSignature = computeHmac(encodedPayload);
        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Invalid signature on room access token");
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            String[] fields = payload.split(":", 4);
            if (fields.length < 3) {
                return null;
            }

            String tokenRoomId = fields[0];
            String username = fields[1];
            long expiresAt = Long.parseLong(fields[2]);

            if (Instant.now().getEpochSecond() > expiresAt) {
                log.warn("Room access token has expired for user '{}' in room '{}'", username, tokenRoomId);
                return null;
            }

            if (expectedRoomId != null && !tokenRoomId.equalsIgnoreCase(expectedRoomId.trim())) {
                log.warn("Room token scope mismatch: expected '{}', got '{}'", expectedRoomId, tokenRoomId);
                return null;
            }

            return new TokenClaims(tokenRoomId, username, expiresAt);
        } catch (Exception ex) {
            log.warn("Failed to parse room access token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Validates whether a token is valid for a given roomId and username.
     */
    public boolean isValid(String token, String expectedRoomId, String expectedUsername) {
        TokenClaims claims = validateToken(token, expectedRoomId);
        if (claims == null) {
            return false;
        }
        if (expectedUsername != null && !expectedUsername.trim().equalsIgnoreCase(claims.getUsername())) {
            return false;
        }
        return true;
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception ex) {
            throw new RuntimeException("HMAC computation failed", ex);
        }
    }

    public static class TokenClaims {
        private final String roomId;
        private final String username;
        private final long expiresAt;

        public TokenClaims(String roomId, String username, long expiresAt) {
            this.roomId = roomId;
            this.username = username;
            this.expiresAt = expiresAt;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getUsername() {
            return username;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
