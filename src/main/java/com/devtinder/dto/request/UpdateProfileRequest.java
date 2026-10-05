package com.devtinder.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProfileRequest(
        @Size(max = 120) String name,
        @Size(max = 260) String githubUrl,
        @Size(max = 600) String bio,
        @Size(max = 160) String lookingFor,
        @Size(max = 120) String location,
        @Size(max = 600) String projectPitch,
        List<String> skills
) {
    public UpdateProfileRequest(
            String name,
            String githubUrl,
            String bio,
            String lookingFor,
            String location,
            List<String> skills
    ) {
        this(name, githubUrl, bio, lookingFor, location, null, skills);
    }
}
