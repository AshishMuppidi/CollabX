package com.collabx.codeexecution.service;

import com.collabx.codeexecution.dto.ExecuteCodeRequest;
import com.collabx.codeexecution.dto.ExecutionResultResponse;
import com.collabx.common.exception.BadRequestException;
import com.collabx.common.security.RoomTokenService;
import com.collabx.config.Judge0Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Judge0ServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private Judge0Properties properties;
    private RoomTokenService roomTokenService;
    private Judge0Service judge0Service;

    @BeforeEach
    void setUp() {
        properties = new Judge0Properties();
        roomTokenService = new RoomTokenService("01234567890123456789012345678901");
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        judge0Service = new Judge0Service(properties, restTemplateBuilder, messagingTemplate, roomTokenService);
    }

    @Test
    @DisplayName("HTML preview executes in browser runner without external API call")
    void testHtmlPreviewExecution() {
        String token = roomTokenService.generateToken("ROOM_HTML", "Alice");
        ExecuteCodeRequest request = new ExecuteCodeRequest(
                "ROOM_HTML",
                "<h1>Hello World</h1>",
                "html",
                null,
                "",
                "Alice",
                token
        );

        ExecutionResultResponse response = judge0Service.executeCode(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("browser-preview", response.getRunner());
        assertEquals("<h1>Hello World</h1>", response.getPreview());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/room.ROOM_HTML.execution"), any(Object.class));
    }

    @Test
    @DisplayName("Execution without valid room token does not broadcast to room")
    void testExecutionWithoutValidToken_DoesNotBroadcast() {
        ExecuteCodeRequest request = new ExecuteCodeRequest(
                "ROOM_HTML",
                "<h1>Hello World</h1>",
                "html",
                null,
                "",
                "Alice",
                "invalid-token"
        );

        ExecutionResultResponse response = judge0Service.executeCode(request);

        assertNotNull(response);
        // Result is returned to caller, but NOT broadcast to the room
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/room.ROOM_HTML.execution"), any(Object.class));
    }

    @Test
    @DisplayName("Unsupported language throws BadRequestException")
    void testUnsupportedLanguage() {
        ExecuteCodeRequest request = new ExecuteCodeRequest(
                "ROOM_UNKNOWN",
                "print 'hello'",
                "brainfuck",
                null,
                "",
                "Bob"
        );

        assertThrows(BadRequestException.class, () -> judge0Service.executeCode(request));
    }
}
