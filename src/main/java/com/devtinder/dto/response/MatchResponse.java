package com.devtinder.dto.response;

import java.time.Instant;

public record MatchResponse(
        Long id,
        ProfileResponse user,
        Instant matchedAt,
        boolean matched,
        long unreadCount
) {
    public MatchResponse(Long id, ProfileResponse user, Instant matchedAt, boolean matched) {
        this(id, user, matchedAt, matched, 0L);
    }
}
