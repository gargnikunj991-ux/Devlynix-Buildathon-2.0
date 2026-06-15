package com.devtinder.websocket;

import com.devtinder.dto.request.ChatMessageRequest;
import com.devtinder.dto.response.MessageResponse;
import com.devtinder.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageHandler {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageHandler(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid ChatMessageRequest request) {
        if (request.senderEmail() == null || request.senderEmail().isBlank()) {
            throw new IllegalArgumentException("senderEmail is required for websocket chat");
        }

        MessageResponse response = chatService.sendMessage(
                request.senderEmail(),
                request.matchId(),
                request.content()
        );

        messagingTemplate.convertAndSend("/topic/matches/" + request.matchId(), response);
    }
}
