package com.devtinder.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(@org.springframework.beans.factory.annotation.Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void registerSession(String userIdentifier, String sessionId) {
        if (userIdentifier == null || sessionId == null) return;

        userToSessions.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToUser.put(sessionId, userIdentifier);

        log.debug("User [{}] connected session [{}]", userIdentifier, sessionId);
        broadcastPresence();
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) return;

        String userIdentifier = sessionToUser.remove(sessionId);
        if (userIdentifier != null) {
            Set<String> sessions = userToSessions.get(userIdentifier);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userToSessions.remove(userIdentifier);
                }
            }
            log.debug("Session [{}] disconnected for user [{}]", sessionId, userIdentifier);
            broadcastPresence();
        }
    }

    public boolean isUserOnline(String userIdentifier) {
        Set<String> sessions = userToSessions.get(userIdentifier);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(userToSessions.keySet());
    }

    private void broadcastPresence() {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/presence", Map.of(
                        "type", "PRESENCE_UPDATE",
                        "onlineUsers", getOnlineUsers()
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
