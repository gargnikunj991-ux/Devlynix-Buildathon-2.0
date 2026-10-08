package com.devtinder.dto.response;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String deviceInfo,
        String ipAddress,
        Instant lastActive,
        Instant createdAt,
        boolean current
) {
}
