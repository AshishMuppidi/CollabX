package com.collabx.collaboration.controller;

import com.collabx.collaboration.model.*;
import com.collabx.collaboration.service.CollaborationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class CollaborationWsController {

    private final CollaborationService collaborationService;

    public CollaborationWsController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId,
                         @Payload Map<String, String> payload,
                         SimpMessageHeaderAccessor headerAccessor) {
        String username = payload.get("username");
        String sessionId = headerAccessor.getSessionId();
        collaborationService.userJoined(sessionId, roomId, username);
    }

    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(@DestinationVariable String roomId,
                          SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        collaborationService.userLeft(sessionId);
    }

    @MessageMapping("/room/{roomId}/code")
    public void syncCode(@DestinationVariable String roomId,
                         @Payload CodeSyncDto codeSyncDto) {
        codeSyncDto.setRoomId(roomId);
        collaborationService.broadcastCode(roomId, codeSyncDto);
    }

    @MessageMapping("/room/{roomId}/language")
    public void changeLanguage(@DestinationVariable String roomId,
                               @Payload LanguageChangeDto languageDto) {
        languageDto.setRoomId(roomId);
        if (languageDto.getFileName() == null || languageDto.getFileName().isEmpty()) {
            languageDto.setFileName(CollaborationService.getFileNameForLanguage(languageDto.getLanguage()));
        }
        collaborationService.setRoomLanguage(roomId, languageDto);
    }

    @MessageMapping("/room/{roomId}/chat")
    public void sendChat(@DestinationVariable String roomId,
                         @Payload ChatMessageDto chatMessage) {
        chatMessage.setRoomId(roomId);
        collaborationService.broadcastChat(roomId, chatMessage);
    }

    @MessageMapping("/room/{roomId}/typing")
    public void sendTyping(@DestinationVariable String roomId,
                           @Payload TypingDto typingDto) {
        typingDto.setRoomId(roomId);
        collaborationService.broadcastTyping(roomId, typingDto);
    }

    @MessageMapping("/room/{roomId}/preview")
    public void syncPreview(@DestinationVariable String roomId,
                            @Payload HtmlPreviewDto previewDto) {
        previewDto.setRoomId(roomId);
        collaborationService.broadcastPreview(roomId, previewDto);
    }
}
