package com.devtinder.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class PresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);

    private final PresenceService presenceService;

    public PresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal principal = headerAccessor.getUser();

        String userIdentifier = null;
        if (principal != null) {
            userIdentifier = principal.getName();
        } else if (headerAccessor.getFirstNativeHeader("userEmail") != null) {
            userIdentifier = headerAccessor.getFirstNativeHeader("userEmail");
        }

        if (userIdentifier != null && sessionId != null) {
            presenceService.registerSession(userIdentifier, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            presenceService.removeSession(sessionId);
        }
    }
}
