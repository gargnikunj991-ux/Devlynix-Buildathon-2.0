package com.devtinder.service;

import com.devtinder.dto.response.DiscoverResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.entity.Skill;
import com.devtinder.entity.Swipe;
import com.devtinder.entity.User;
import com.devtinder.exception.ResourceNotFoundException;
import com.devtinder.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoverService {

    private final UserRepository userRepository;
    private final MatchService matchService;

    public DiscoverService(UserRepository userRepository, MatchService matchService) {
        this.userRepository = userRepository;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public List<DiscoverResponse> discover(String email, String requiredSkill) {
        User currentUser = findByEmail(email);
        Set<String> currentSkills = lowerSkillSet(currentUser);
        String required = normalizeSkill(requiredSkill);

        return userRepository.findDiscoverableUsers(currentUser.getId()).stream()
                .filter(candidate -> required == null || lowerSkillSet(candidate).contains(required))
                .map(candidate -> toDiscoverResponse(candidate, currentSkills))
                .sorted(Comparator.comparingInt(DiscoverResponse::sharedSkillCount).reversed())
                .toList();
    }

    @Transactional
    public com.devtinder.dto.response.MatchResponse swipe(String email, Long targetUserId, String direction) {
        User currentUser = findByEmail(email);
        Swipe.Direction swipeDirection = parseDirection(direction);
        return matchService.swipe(currentUser, targetUserId, swipeDirection);
    }

    private DiscoverResponse toDiscoverResponse(User candidate, Set<String> currentSkills) {
        List<String> sharedSkills = candidate.getSkills().stream()
                .map(Skill::getName)
                .filter(skill -> currentSkills.contains(skill.toLowerCase(Locale.ROOT)))
                .sorted()
                .toList();

        return new DiscoverResponse(ProfileResponse.from(candidate), sharedSkills.size(), sharedSkills);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    private static Set<String> lowerSkillSet(User user) {
        return user.getSkills().stream()
                .map(Skill::getName)
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static String normalizeSkill(String skill) {
        if (skill == null || skill.isBlank()) {
            return null;
        }
        return skill.trim().toLowerCase(Locale.ROOT);
    }

    private static Swipe.Direction parseDirection(String direction) {
        try {
            return Swipe.Direction.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Swipe direction must be LIKE or PASS");
        }
    }
}
