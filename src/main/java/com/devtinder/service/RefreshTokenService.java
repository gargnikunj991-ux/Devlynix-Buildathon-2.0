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

        RefreshToken token = refreshTokenRepository.findByToken(rawToken.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // RTR Grace Period and Hacker Theft / Replay attack detection:
        if (token.isRevoked()) {
            Instant revokedAt = token.getRevokedAt();
            // Allow 30 seconds grace period for network latency / concurrent browser requests
            if (revokedAt != null
                    && Duration.between(revokedAt, Instant.now()).getSeconds() < 30
                    && token.getReplacedByToken() != null) {
                RefreshToken replacement = refreshTokenRepository.findByToken(token.getReplacedByToken())
                        .orElse(null);
                if (replacement != null && !replacement.isRevoked() && replacement.getExpiryDate().isAfter(Instant.now())) {
                    log.info("Concurrent refresh within 30s grace period for family [{}]. Returning active replacement token.", token.getFamilyId());
                    return new RotationResult(replacement.getUser(), replacement.getToken());
                }
            }

            // Outside grace period: genuine replay attack! Destroy family to defend user
            log.warn("SECURITY ALERT: Spent refresh token re-submitted outside grace period for family [{}]. Deleting compromised token family.", token.getFamilyId());
            refreshTokenRepository.deleteByFamilyId(token.getFamilyId());
            throw new IllegalArgumentException("Security compromise detected. All sessions in this lineage have been revoked.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByFamilyId(token.getFamilyId());
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
    public void deleteTokenSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(rawToken.trim()).ifPresentOrElse(t -> {
            String familyId = t.getFamilyId();
            refreshTokenRepository.deleteByFamilyId(familyId);
            log.info("Deleted refresh token session family [{}] on logout for user [{}]", familyId, t.getUser().getEmail());
        }, () -> {
            refreshTokenRepository.deleteByToken(rawToken.trim());
            log.info("Deleted refresh token directly on logout: [{}]", rawToken);
        });
    }

    @Transactional
    public void revokeToken(String rawToken) {
        deleteTokenSession(rawToken);
    }

    @Transactional
    public void deleteAllUserTokens(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("Deleted all active refresh tokens for user [{}]", user.getEmail());
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        deleteAllUserTokens(user);
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
                refreshTokenRepository.deleteByFamilyId(t.getFamilyId());
                log.info("User [{}] deleted session family [{}]", user.getEmail(), t.getFamilyId());
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
            refreshTokenRepository.deleteOtherFamiliesByUserId(user.getId(), currentFamilyId);
            log.info("User [{}] deleted all other session families except [{}]", user.getEmail(), currentFamilyId);
        });
    }

    @Transactional
    public int purgeAllRevokedAndExpiredTokens() {
        Instant now = Instant.now();
        int deleted = refreshTokenRepository.purgeAllRevokedOrExpiredTokens(now);
        if (deleted > 0) {
            log.info("Purged {} revoked/expired refresh tokens from database", deleted);
        }
        return deleted;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void onStartupCleanup() {
        try {
            int purged = purgeAllRevokedAndExpiredTokens();
            if (purged > 0) {
                log.info("Startup cleanup completed: purged {} revoked/expired tokens from database", purged);
            }
        } catch (Exception e) {
            log.warn("Startup cleanup encountered error: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000, initialDelay = 10000) // Runs 10s after startup, then every 5 minutes
    @Transactional
    public void purgeExpiredAndStaleTokens() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(1)); // Allow 1-minute grace period for rotated tokens
        int deleted = refreshTokenRepository.purgeOldTokens(now, cutoff);
        if (deleted > 0) {
            log.info("Automated cleanup: purged {} expired/stale refresh tokens from database", deleted);
        }
    }
}
