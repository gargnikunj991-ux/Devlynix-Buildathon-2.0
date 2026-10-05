package com.devtinder.dto.response;

import java.util.List;

public record DiscoverResponse(
        ProfileResponse profile,
        int sharedSkillCount,
        List<String> sharedSkills,
        int synergyScore
) {
    public DiscoverResponse(ProfileResponse profile, int sharedSkillCount, List<String> sharedSkills) {
        this(profile, sharedSkillCount, sharedSkills, Math.min(99, Math.max(50, 65 + sharedSkillCount * 8)));
    }
}
