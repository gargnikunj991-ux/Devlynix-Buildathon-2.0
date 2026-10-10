package com.devtinder.service;

import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.dto.response.SessionResponse;
import com.devtinder.entity.RefreshToken;
import com.devtinder.entity.Skill;
import com.devtinder.entity.User;
import com.devtinder.repository.SkillRepository;
import com.devtinder.repository.UserRepository;
import com.devtinder.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            SkillRepository skillRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGithubUrl(blankToNull(request.githubUrl()));
        user.setProjectPitch(blankToNull(request.projectPitch()));
        user.setSkills(resolveSkills(request.skills()));

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(toUserDetails(saved));
        RefreshToken refreshToken = refreshTokenService.createSession(saved, httpRequest);

        return new AuthResponse(token, refreshToken.getToken(), ProfileResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createSession(user, httpRequest);

        return new AuthResponse(token, refreshToken.getToken(), ProfileResponse.from(user));
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, HttpServletRequest httpRequest) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotateToken(rawRefreshToken, httpRequest);
        UserDetails userDetails = toUserDetails(result.user());
        String newAccessToken = jwtService.generateToken(userDetails);

        return new AuthResponse(newAccessToken, result.newRefreshToken(), ProfileResponse.from(result.user()));
    }

    @Transactional
    public void logout(String rawRefreshToken, String userEmail) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.deleteTokenSession(rawRefreshToken.trim());
        } else if (userEmail != null && !userEmail.isBlank()) {
            userRepository.findByEmail(normalizeEmail(userEmail))
                    .ifPresent(refreshTokenService::deleteAllUserTokens);
        }
        refreshTokenService.purgeAllRevokedAndExpiredTokens();
    }

    @Transactional
    public int cleanupTokens() {
        return refreshTokenService.purgeAllRevokedAndExpiredTokens();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        logout(rawRefreshToken, null);
    }

    @Transactional
    public void logoutUser(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .ifPresent(refreshTokenService::deleteAllUserTokens);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(String email, String currentRefreshToken) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return refreshTokenService.getActiveSessions(user, currentRefreshToken);
    }

    @Transactional
    public void terminateSession(String email, Long sessionId) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        refreshTokenService.terminateSession(user, sessionId);
    }

    @Transactional
    public void terminateOtherSessions(String email, String currentRefreshToken) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        refreshTokenService.terminateOtherSessions(user, currentRefreshToken);
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
