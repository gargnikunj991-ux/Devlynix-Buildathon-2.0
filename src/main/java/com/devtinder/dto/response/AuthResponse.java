package com.devtinder.dto.response;

public record AuthResponse(
        String token,
        ProfileResponse user
) {
}
