package com.devtinder.dto.response;

import java.time.Instant;

public record MatchResponse(
        Long id,
        ProfileResponse user,
        Instant matchedAt,
        boolean matched
) {
}
