package com.devtinder.dto.response;

import com.devtinder.entity.Skill;
import com.devtinder.entity.User;
import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String githubUrl,
        String bio,
        String lookingFor,
        String location,
        String projectPitch,
        List<String> skills,
        Instant createdAt
) {
    public ProfileResponse(
            Long id,
            String name,
            String email,
            String githubUrl,
            String bio,
            String lookingFor,
            String location,
            List<String> skills,
            Instant createdAt
    ) {
        this(id, name, email, githubUrl, bio, lookingFor, location, null, skills, createdAt);
    }

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getGithubUrl(),
                user.getBio(),
                user.getLookingFor(),
                user.getLocation(),
                user.getProjectPitch(),
                user.getSkills().stream().map(Skill::getName).sorted().toList(),
                user.getCreatedAt()
        );
    }
}
