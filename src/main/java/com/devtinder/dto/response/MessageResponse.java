package com.devtinder.dto.response;

import com.devtinder.entity.Message;
import java.time.Instant;

public record MessageResponse(
        Long id,
        Long matchId,
        Long senderId,
        String senderName,
        String content,
        Instant sentAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getMatch().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
