package com.devtinder.service;

import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.entity.Skill;
import com.devtinder.entity.User;
import com.devtinder.repository.SkillRepository;
import com.devtinder.repository.UserRepository;
import com.devtinder.security.JwtService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            SkillRepository skillRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGithubUrl(blankToNull(request.githubUrl()));
        user.setSkills(resolveSkills(request.skills()));

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(toUserDetails(saved));

        return new AuthResponse(token, ProfileResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        return new AuthResponse(jwtService.generateToken(userDetails), ProfileResponse.from(user));
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        Set<Skill> skills = new LinkedHashSet<>();
        if (skillNames == null) {
            return skills;
        }

        skillNames.stream()
                .map(AuthService::blankToNull)
                .filter(name -> name != null)
                .map(AuthService::titleCaseSkill)
                .distinct()
                .map(name -> skillRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> skillRepository.save(new Skill(name))))
                .forEach(skills::add);

        return skills;
    }

    private static UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
