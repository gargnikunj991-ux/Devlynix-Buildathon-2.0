package com.devtinder.controller;

import com.devtinder.dto.request.ChatMessageRequest;
import com.devtinder.dto.response.MessageResponse;
import com.devtinder.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{matchId}/messages")
    public List<MessageResponse> messages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId
    ) {
        return chatService.getMessages(userDetails.getUsername(), matchId);
    }

    @PostMapping("/{matchId}/messages")
    public MessageResponse send(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return chatService.sendMessage(userDetails.getUsername(), matchId, request.content());
    }
}
