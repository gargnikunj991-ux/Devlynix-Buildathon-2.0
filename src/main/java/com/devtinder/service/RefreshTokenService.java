package com.devtinder.service;

import com.devtinder.dto.response.SessionResponse;
import com.devtinder.entity.RefreshToken;
import com.devtinder.entity.User;
import com.devtinder.repository.RefreshTokenRepository;
import com.devtinder.security.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshExpiration;

    public record RotationResult(User user, String newRefreshToken) {}

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-days:7}") long refreshExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = Duration.ofDays(refreshExpirationDays);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Transactional
    public RefreshToken createSession(User user, HttpServletRequest request) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(generateSecureToken());
        token.setFamilyId(UUID.randomUUID().toString());
        token.setDeviceInfo(UserAgentUtil.extractDeviceInfo(request));
        token.setIpAddress(UserAgentUtil.extractClientIp(request));
        token.setExpiryDate(Instant.now().plus(refreshExpiration));
        token.setRevoked(false);
        token.setLastActive(Instant.now());
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RotationResult rotateToken(String rawToken, HttpServletRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing");
        }

        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Hacker race condition / Replay attack detection:
        // If a previously revoked/spent token is submitted again, someone compromised the token!
        if (token.isRevoked()) {
            log.warn("SECURITY ALERT: Spent refresh token re-submitted for family [{}]. Revoking entire token family.", token.getFamilyId());
            refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
            throw new IllegalArgumentException("Security compromise detected. All sessions in this lineage have been revoked.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        String newTokenString = generateSecureToken();
        Instant now = Instant.now();

        // 1. Mark existing token as revoked, keeping it for the detection window
        token.setRevoked(true);
        token.setRevokedAt(now);
        token.setReplacedByToken(newTokenString);
        refreshTokenRepository.save(token);

        // 2. Issue rotated token continuing the same device lineage / family
        RefreshToken nextToken = new RefreshToken();
        nextToken.setUser(token.getUser());
        nextToken.setFamilyId(token.getFamilyId());
        nextToken.setToken(newTokenString);
        nextToken.setDeviceInfo(UserAgentUtil.extractDeviceInfo(request));
        nextToken.setIpAddress(UserAgentUtil.extractClientIp(request));
        nextToken.setExpiryDate(now.plus(refreshExpiration));
        nextToken.setRevoked(false);
        nextToken.setLastActive(now);

        RefreshToken saved = refreshTokenRepository.save(nextToken);
        return new RotationResult(saved.getUser(), saved.getToken());
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(rawToken.trim()).ifPresentOrElse(t -> {
            refreshTokenRepository.revokeFamily(t.getFamilyId(), Instant.now());
            log.info("Revoked session family [{}] on logout for user [{}]", t.getFamilyId(), t.getUser().getEmail());
        }, () -> {
            log.warn("Logout requested for refresh token that was not found in database: [{}]", rawToken);
        });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user.getId(), Instant.now());
        log.info("Revoked all active refresh tokens for user [{}]", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(User user, String currentRefreshToken) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalseOrderByLastActiveDesc(user);
        return activeTokens.stream()
                .map(t -> new SessionResponse(
                        t.getId(),
                        t.getDeviceInfo(),
                        t.getIpAddress(),
                        t.getLastActive(),
                        t.getCreatedAt(),
                        currentRefreshToken != null && currentRefreshToken.equals(t.getToken())
                ))
                .toList();
    }

    @Transactional
    public void terminateSession(User user, Long sessionId) {
        refreshTokenRepository.findById(sessionId).ifPresent(t -> {
            if (t.getUser().getId().equals(user.getId())) {
                refreshTokenRepository.revokeFamily(t.getFamilyId(), Instant.now());
                log.info("User [{}] revoked session family [{}]", user.getEmail(), t.getFamilyId());
            }
        });
    }

    @Transactional
    public void terminateOtherSessions(User user, String currentRefreshToken) {
        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(currentRefreshToken.trim()).ifPresent(currentToken -> {
            String currentFamilyId = currentToken.getFamilyId();
            List<RefreshToken> allTokens = refreshTokenRepository.findByUserAndRevokedFalseOrderByLastActiveDesc(user);
            for (RefreshToken t : allTokens) {
                if (!t.getFamilyId().equals(currentFamilyId)) {
                    refreshTokenRepository.revokeFamily(t.getFamilyId(), Instant.now());
                }
            }
            log.info("User [{}] revoked all other session families except [{}]", user.getEmail(), currentFamilyId);
        });
    }

    @Scheduled(fixedRate = 3600000) // Hourly background cleanup
    @Transactional
    public void purgeExpiredAndStaleTokens() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofHours(24)); // Retain revoked tokens for 24h to detect replay attacks
        int deleted = refreshTokenRepository.purgeOldTokens(now, cutoff);
        if (deleted > 0) {
            log.info("Automated cleanup: purged {} expired/stale refresh tokens from database", deleted);
        }
    }
}
