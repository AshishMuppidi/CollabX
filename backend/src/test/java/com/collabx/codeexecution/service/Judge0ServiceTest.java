package com.collabx.codeexecution.service;

import com.collabx.codeexecution.dto.ExecuteCodeRequest;
import com.collabx.codeexecution.dto.ExecutionResultResponse;
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

@ExtendWith(MockitoExtension.class)
class Judge0ServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private Judge0Properties properties;
    private Judge0Service judge0Service;

    @BeforeEach
    void setUp() {
        properties = new Judge0Properties();
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        judge0Service = new Judge0Service(properties, restTemplateBuilder, messagingTemplate);
    }

    @Test
    @DisplayName("HTML preview executes in browser runner without external API call")
    void testHtmlPreviewExecution() {
        ExecuteCodeRequest request = new ExecuteCodeRequest(
                "ROOM_HTML",
                "<h1>Hello World</h1>",
                "html",
                null,
                "",
                "Alice"
        );

        ExecutionResultResponse response = judge0Service.executeCode(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("browser-preview", response.getRunner());
        assertEquals("<h1>Hello World</h1>", response.getPreview());
    }

    @Test
    @DisplayName("Unsupported language returns error response")
    void testUnsupportedLanguage() {
        ExecuteCodeRequest request = new ExecuteCodeRequest(
                "ROOM_UNKNOWN",
                "print 'hello'",
                "brainfuck",
                null,
                "",
                "Bob"
        );

        ExecutionResultResponse response = judge0Service.executeCode(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getStderr().contains("Unsupported programming language") ||
                response.getMessage().contains("error"));
    }
}
