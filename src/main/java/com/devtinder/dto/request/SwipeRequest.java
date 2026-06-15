package com.devtinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SwipeRequest(
        @NotNull Long targetUserId,
        @NotBlank String direction
) {
}
