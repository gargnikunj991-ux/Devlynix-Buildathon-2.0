package com.devtinder.service;

import com.devtinder.dto.request.UpdateProfileRequest;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.entity.Skill;
import com.devtinder.entity.User;
import com.devtinder.exception.ResourceNotFoundException;
import com.devtinder.repository.SkillRepository;
import com.devtinder.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public ProfileService(UserRepository userRepository, SkillRepository skillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        return ProfileResponse.from(findByEmail(email));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.githubUrl() != null) {
            user.setGithubUrl(blankToNull(request.githubUrl()));
        }
        if (request.bio() != null) {
            user.setBio(blankToNull(request.bio()));
        }
        if (request.lookingFor() != null) {
            user.setLookingFor(blankToNull(request.lookingFor()));
        }
        if (request.location() != null) {
            user.setLocation(blankToNull(request.location()));
        }
        if (request.skills() != null) {
            user.setSkills(resolveSkills(request.skills()));
        }

        return ProfileResponse.from(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        Set<Skill> skills = new LinkedHashSet<>();
        skillNames.stream()
                .map(ProfileService::blankToNull)
                .filter(name -> name != null)
                .map(ProfileService::titleCaseSkill)
                .distinct()
                .map(name -> skillRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> skillRepository.save(new Skill(name))))
                .forEach(skills::add);
        return skills;
    }

    private static String titleCaseSkill(String skill) {
        String trimmed = skill.trim();
        if (trimmed.length() <= 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
