package com.devtinder.dto.response;

import com.devtinder.entity.Message;
import java.time.Instant;

public record MessageResponse(
        Long id,
        Long matchId,
        Long senderId,
        String senderName,
        String content,
        Instant sentAt,
        boolean isRead,
        Instant readAt
) {
    public MessageResponse(Long id, Long matchId, Long senderId, String senderName, String content, Instant sentAt) {
        this(id, matchId, senderId, senderName, content, sentAt, false, null);
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getMatch().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getSentAt(),
                message.isRead(),
                message.getReadAt()
        );
    }
}
