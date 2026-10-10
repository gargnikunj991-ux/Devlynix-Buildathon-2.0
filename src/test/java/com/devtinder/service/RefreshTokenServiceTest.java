package com.devtinder.service;

import com.devtinder.entity.RefreshToken;
import com.devtinder.entity.User;
import com.devtinder.repository.RefreshTokenRepository;
import com.devtinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setName("Token Tester");
        user.setEmail("tester@devtinder.com");
        user.setPasswordHash("hash123");
        testUser = userRepository.save(user);
    }

    @Test
    void testLogoutPhysicallyDeletesRefreshTokenFromDatabaseTable() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Tests");

        RefreshToken session = refreshTokenService.createSession(testUser, request);
        assertNotNull(session.getId());
        assertTrue(refreshTokenRepository.findByToken(session.getToken()).isPresent());

        // Perform logout
        refreshTokenService.deleteTokenSession(session.getToken());

        // Verify the token is completely deleted from the database table
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findByToken(session.getToken());
        assertTrue(deletedToken.isEmpty(), "Refresh token must be deleted from the database table upon logout");
        assertTrue(refreshTokenRepository.findByFamilyId(session.getFamilyId()).isEmpty(),
                "Entire token family must be deleted from database upon logout");
    }

    @Test
    void testRefreshTokenRotationAndGracePeriod() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        RefreshToken initial = refreshTokenService.createSession(testUser, request);
        String initialToken = initial.getToken();

        // 1. Rotate token
        RefreshTokenService.RotationResult firstRotation = refreshTokenService.rotateToken(initialToken, request);
        assertNotNull(firstRotation);
        assertNotEquals(initialToken, firstRotation.newRefreshToken());

        // 2. Submit previous token again within 30-second grace window (simulating parallel client refresh)
        RefreshTokenService.RotationResult gracePeriodResult = refreshTokenService.rotateToken(initialToken, request);
        assertNotNull(gracePeriodResult);
        assertEquals(firstRotation.newRefreshToken(), gracePeriodResult.newRefreshToken(),
                "Concurrent refresh within grace period must return active replacement token without failing");

        // 3. Newly rotated token is also valid
        RefreshTokenService.RotationResult secondRotation = refreshTokenService.rotateToken(firstRotation.newRefreshToken(), request);
        assertNotNull(secondRotation);
    }

    @Test
    void testLogoutAllDeletesAllTokensForUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        refreshTokenService.createSession(testUser, request);
        refreshTokenService.createSession(testUser, request);

        assertEquals(2, refreshTokenRepository.count());

        // Logout everywhere
        refreshTokenService.deleteAllUserTokens(testUser);

        assertEquals(0, refreshTokenRepository.count(), "All refresh tokens for user must be deleted from table");
    }
}
