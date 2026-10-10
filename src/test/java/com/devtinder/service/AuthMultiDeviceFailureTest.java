package com.devtinder.service;

import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.entity.RefreshToken;
import com.devtinder.repository.RefreshTokenRepository;
import com.devtinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthMultiDeviceFailureTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("FAILING TEST: Logging out of Laptop should NOT kill Mobile device session")
    void testSingleDeviceLogoutDoesNotKillOtherDeviceSessions() {
        // 1. Register Alice
        RegisterRequest registerReq = new RegisterRequest(
                "Alice Engineer",
                "alice@example.com",
                "SecurePass123!",
                "https://github.com/alice",
                "Building cool APIs",
                List.of("Java", "Spring Boot")
        );
        MockHttpServletRequest initRequest = new MockHttpServletRequest();
        authService.register(registerReq, initRequest);

        // 2. Alice logs in on Laptop (Device 1)
        MockHttpServletRequest laptopRequest = new MockHttpServletRequest();
        laptopRequest.setRemoteAddr("192.168.1.10");
        laptopRequest.addHeader("User-Agent", "Chrome on MacOS Laptop");
        AuthResponse laptopAuth = authService.login(new LoginRequest("alice@example.com", "SecurePass123!"), laptopRequest);
        String laptopRefreshToken = laptopAuth.refreshToken();

        // 3. Alice logs in on Mobile Phone (Device 2)
        MockHttpServletRequest phoneRequest = new MockHttpServletRequest();
        phoneRequest.setRemoteAddr("192.168.1.55");
        phoneRequest.addHeader("User-Agent", "Safari on iPhone");
        AuthResponse phoneAuth = authService.login(new LoginRequest("alice@example.com", "SecurePass123!"), phoneRequest);
        String phoneRefreshToken = phoneAuth.refreshToken();

        assertNotEquals(laptopRefreshToken, phoneRefreshToken);
        assertEquals(3, refreshTokenRepository.count(), "Should have 3 active session tokens in DB");

        // 4. Alice logs out from LAPTOP ONLY, providing Laptop's refresh token and her email
        // (This is exactly what AuthController does when hitting POST /api/auth/logout with Bearer token)
        authService.logout(laptopRefreshToken, "alice@example.com");

        // 5. Laptop token MUST be deleted from database table
        Optional<RefreshToken> laptopDbToken = refreshTokenRepository.findByToken(laptopRefreshToken);
        assertTrue(laptopDbToken.isEmpty(), "Laptop refresh token must be deleted upon logout");

        // 6. EXPECTED: Phone token MUST STILL BE ALIVE and usable on the phone!
        // This assertion will FAIL if authService.logout accidentally wiped all user tokens!
        Optional<RefreshToken> phoneDbToken = refreshTokenRepository.findByToken(phoneRefreshToken);
        assertTrue(phoneDbToken.isPresent(),
                "FAILURE DETECTED: Mobile phone session was destroyed when logging out of Laptop! Multi-device isolation failed!");
        assertFalse(phoneDbToken.get().isRevoked(), "Phone token should remain active and unrevoked");
    }
}
