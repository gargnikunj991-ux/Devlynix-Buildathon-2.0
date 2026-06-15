package com.devtinder.dto.response;

import java.util.List;

public record DiscoverResponse(
        ProfileResponse profile,
        int sharedSkillCount,
        List<String> sharedSkills
) {
}
