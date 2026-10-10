package com.devtinder.controller;

import com.devtinder.websocket.PresenceService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public Map<String, Object> getPresence() {
        Set<String> online = presenceService.getOnlineUsers();
        return Map.of(
                "onlineUsers", online,
                "count", online.size()
        );
    }

    @MessageMapping("/presence.heartbeat")
    public void heartbeat(
            @Header(value = "simpSessionId", required = false) String sessionId,
            Map<String, String> payload
    ) {
        String email = payload != null ? payload.get("email") : null;
        if (email != null && sessionId != null) {
            presenceService.registerSession(email.trim().toLowerCase(), sessionId);
        }
    }
}
