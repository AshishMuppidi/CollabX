package com.collabx.collaboration;

import com.collabx.collaboration.model.ChatMessageDto;
import com.collabx.collaboration.model.CodeSyncDto;
import com.collabx.collaboration.model.LanguageChangeDto;
import com.collabx.common.security.RoomTokenService;
import com.collabx.room.dto.CreateRoomRequest;
import com.collabx.room.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TwoClientCollaborationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomTokenService roomTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private String wsUrl;

    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
        try {
            roomService.createRoom(new CreateRoomRequest("ROOM_ALPHA", "Alice", "password123"));
        } catch (Exception ignored) {}
        try {
            roomService.createRoom(new CreateRoomRequest("ROOM_BETA", "Eve", "password123"));
        } catch (Exception ignored) {}
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
        return client;
    }

    private StompHeaders createHeaders(String destination, String token) {
        StompHeaders headers = new StompHeaders();
        if (destination != null) {
            headers.setDestination(destination);
        }
        if (token != null) {
            headers.add("X-Room-Token", token);
        }
        return headers;
    }

    @Test
    @DisplayName("Two clients in same room exchange code and chat, while client in different room is isolated")
    void testTwoClientCollaborationAndCrossRoomIsolation() throws Exception {
        String tokenAlice = roomTokenService.generateToken("ROOM_ALPHA", "Alice");
        String tokenBob = roomTokenService.generateToken("ROOM_ALPHA", "Bob");
        String tokenEve = roomTokenService.generateToken("ROOM_BETA", "Eve");

        WebSocketStompClient stompAlice = createStompClient();
        WebSocketStompClient stompBob = createStompClient();
        WebSocketStompClient stompEve = createStompClient();

        BlockingQueue<ChatMessageDto> bobChatQueue = new LinkedBlockingQueue<>();
        BlockingQueue<CodeSyncDto> bobCodeQueue = new LinkedBlockingQueue<>();
        BlockingQueue<LanguageChangeDto> bobLangQueue = new LinkedBlockingQueue<>();

        BlockingQueue<ChatMessageDto> eveChatQueue = new LinkedBlockingQueue<>();
        BlockingQueue<CodeSyncDto> eveCodeQueue = new LinkedBlockingQueue<>();

        // Connect Alice, Bob, Eve
        StompSession sessionAlice = stompAlice.connectAsync(wsUrl, new WebSocketHttpHeaders(), createHeaders(null, tokenAlice), new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        StompSession sessionBob = stompBob.connectAsync(wsUrl, new WebSocketHttpHeaders(), createHeaders(null, tokenBob), new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        StompSession sessionEve = stompEve.connectAsync(wsUrl, new WebSocketHttpHeaders(), createHeaders(null, tokenEve), new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        // Join messages
        sessionAlice.send(createHeaders("/app/room/ROOM_ALPHA/join", tokenAlice), new ChatMessageDto("ROOM_ALPHA", "Alice", "join"));
        sessionBob.send(createHeaders("/app/room/ROOM_ALPHA/join", tokenBob), new ChatMessageDto("ROOM_ALPHA", "Bob", "join"));
        sessionEve.send(createHeaders("/app/room/ROOM_BETA/join", tokenEve), new ChatMessageDto("ROOM_BETA", "Eve", "join"));

        // Bob subscribes to ROOM_ALPHA topics (each with separate StompHeaders)
        sessionBob.subscribe(createHeaders("/topic/room.ROOM_ALPHA.chat", tokenBob), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return ChatMessageDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                bobChatQueue.offer((ChatMessageDto) payload);
            }
        });

        sessionBob.subscribe(createHeaders("/topic/room.ROOM_ALPHA.code", tokenBob), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return CodeSyncDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                bobCodeQueue.offer((CodeSyncDto) payload);
            }
        });

        sessionBob.subscribe(createHeaders("/topic/room.ROOM_ALPHA.language", tokenBob), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return LanguageChangeDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                bobLangQueue.offer((LanguageChangeDto) payload);
            }
        });

        // Eve subscribes to ROOM_BETA topics (each with separate StompHeaders)
        sessionEve.subscribe(createHeaders("/topic/room.ROOM_BETA.chat", tokenEve), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return ChatMessageDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eveChatQueue.offer((ChatMessageDto) payload);
            }
        });

        sessionEve.subscribe(createHeaders("/topic/room.ROOM_BETA.code", tokenEve), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return CodeSyncDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eveCodeQueue.offer((CodeSyncDto) payload);
            }
        });

        Thread.sleep(800);

        // 1. Alice sends a chat message in ROOM_ALPHA
        sessionAlice.send(createHeaders("/app/room/ROOM_ALPHA/chat", tokenAlice), new ChatMessageDto("ROOM_ALPHA", "Alice", "Hello from Alice!"));

        ChatMessageDto receivedChatBob = bobChatQueue.poll(4, TimeUnit.SECONDS);
        assertNotNull(receivedChatBob, "Bob should have received Alice's chat message");
        assertEquals("Hello from Alice!", receivedChatBob.getMessage());
        assertEquals("Alice", receivedChatBob.getUsername());

        // Eve in ROOM_BETA must NOT receive Alice's chat message
        ChatMessageDto receivedChatEve = eveChatQueue.poll(1, TimeUnit.SECONDS);
        assertNull(receivedChatEve, "Eve in ROOM_BETA must NOT receive ROOM_ALPHA chat message");

        // 2. Alice sends code edit in ROOM_ALPHA
        sessionAlice.send(createHeaders("/app/room/ROOM_ALPHA/code", tokenAlice), new CodeSyncDto("ROOM_ALPHA", "const x = 42;", "Alice"));

        CodeSyncDto receivedCodeBob = bobCodeQueue.poll(4, TimeUnit.SECONDS);
        assertNotNull(receivedCodeBob, "Bob should have received Alice's code update");
        assertEquals("const x = 42;", receivedCodeBob.getCode());

        // Eve must NOT receive Alice's code
        CodeSyncDto receivedCodeEve = eveCodeQueue.poll(1, TimeUnit.SECONDS);
        assertNull(receivedCodeEve, "Eve in ROOM_BETA must NOT receive ROOM_ALPHA code sync");

        // 3. Alice switches language to python
        bobLangQueue.clear(); // Clear initial language broadcast received upon room join
        sessionAlice.send(createHeaders("/app/room/ROOM_ALPHA/language", tokenAlice), new LanguageChangeDto("ROOM_ALPHA", "python", "main.py", "Alice"));

        LanguageChangeDto receivedLangBob = bobLangQueue.poll(4, TimeUnit.SECONDS);
        assertNotNull(receivedLangBob, "Bob should have received language change to python");
        assertEquals("python", receivedLangBob.getLanguage());

        // Cleanup sessions
        sessionAlice.disconnect();
        sessionBob.disconnect();
        sessionEve.disconnect();
    }
}
