package com.devtinder.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProfileRequest(
        @Size(max = 120) String name,
        @Size(max = 260) String githubUrl,
        @Size(max = 600) String bio,
        @Size(max = 160) String lookingFor,
        @Size(max = 120) String location,
        List<String> skills
) {
}
