package com.devtinder.dto.response;

public record AuthResponse(
        String token,
        String refreshToken,
        ProfileResponse user
) {
}
