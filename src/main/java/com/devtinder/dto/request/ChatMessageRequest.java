package com.devtinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        long matchId,
        String senderEmail,
        @NotBlank @Size(max = 1200) String content
) {
}