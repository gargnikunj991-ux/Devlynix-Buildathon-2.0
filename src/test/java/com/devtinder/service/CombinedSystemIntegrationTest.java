package com.devtinder.service;

import com.devtinder.dto.request.ChatMessageRequest;
import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.dto.response.MatchResponse;
import com.devtinder.dto.response.MessageResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.dto.response.SessionResponse;
import com.devtinder.entity.RefreshToken;
import com.devtinder.entity.Swipe;
import com.devtinder.entity.User;
import com.devtinder.repository.MatchRepository;
import com.devtinder.repository.MessageRepository;
import com.devtinder.repository.RefreshTokenRepository;
import com.devtinder.repository.SwipeRepository;
import com.devtinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Combined End-to-End Integration Test Suite.
 * Validates the full platform lifecycle:
 * 1. User Registration, Authentication & Multi-Device Sessions
 * 2. Refresh Token Rotation (RTR), Concurrency Grace Window & Theft Replay Defense
 * 3. Session Termination (Single, Remote, Other, Global)
 * 4. User Discovery, Synergy Matching, Swiping & Mutual Matches
 * 5. Real-Time Chat, XSS Input Sanitization & Message History
 * 6. Database Token Cleanup & Neon Physical Deletion
 */
@SpringBootTest
@Transactional
class CombinedSystemIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscoverService discoverService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        matchRepository.deleteAll();
        swipeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("COMBINED TEST 1: Full Auth, Multi-Device, RTR, Grace Window & Replay Theft Defense")
    void testAuthAndTokenLifecycleCombined() {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("10.0.0.1");
        request1.addHeader("User-Agent", "Firefox / Windows PC");

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("10.0.0.2");
        request2.addHeader("User-Agent", "Safari / iPhone 15");

        // 1. Registration
        RegisterRequest registerReq = new RegisterRequest(
                "Alice Engineer",
                "alice@devlynix.com",
                "SecretPass2026!",
                "https://github.com/alice-eng",
                "Full-stack AI developer",
                List.of("Java", "Spring Boot", "React", "Docker")
        );
        AuthResponse aliceReg = authService.register(registerReq, request1);
        assertNotNull(aliceReg.token());
        assertNotNull(aliceReg.refreshToken());

        // Duplicate registration must fail
        assertThrows(IllegalArgumentException.class, () ->
                authService.register(registerReq, request1),
                "Registering with duplicate email must throw IllegalArgumentException"
        );

        // 2. Login on Device 2 (Mobile)
        AuthResponse alicePhone = authService.login(
                new LoginRequest("alice@devlynix.com", "SecretPass2026!"),
                request2
        );
        assertNotNull(alicePhone.token());
        assertNotNull(alicePhone.refreshToken());
        assertNotEquals(aliceReg.refreshToken(), alicePhone.refreshToken());

        // 3. Inspect Active Sessions
        List<SessionResponse> sessions = authService.getSessions("alice@devlynix.com", alicePhone.refreshToken());
        assertEquals(2, sessions.size(), "Alice should have 2 active sessions (PC + Phone)");
        assertTrue(sessions.stream().anyMatch(SessionResponse::current), "One session must be marked as current");

        // 4. RTR - Token Rotation on PC
        RefreshTokenService.RotationResult rotatedResult = refreshTokenService.rotateToken(aliceReg.refreshToken(), request1);
        assertNotNull(rotatedResult.newRefreshToken());
        assertNotEquals(aliceReg.refreshToken(), rotatedResult.newRefreshToken());

        // 5. RTR - Concurrency Grace Period (parallel request with old token within 30s)
        RefreshTokenService.RotationResult graceResult = refreshTokenService.rotateToken(aliceReg.refreshToken(), request1);
        assertEquals(rotatedResult.newRefreshToken(), graceResult.newRefreshToken(),
                "Parallel request within 30s grace window must receive active replacement token");

        // 6. RTR - Replay Attack Theft Defense (compromise detection)
        // Manually simulate token outside grace window (e.g., 2 minutes ago)
        RefreshToken oldToken = refreshTokenRepository.findByToken(aliceReg.refreshToken()).orElseThrow();
        oldToken.setRevokedAt(Instant.now().minus(Duration.ofMinutes(2)));
        refreshTokenRepository.save(oldToken);

        // Attempting to use old token outside grace window MUST trigger theft defense and delete the lineage
        String familyId = oldToken.getFamilyId();
        assertThrows(IllegalArgumentException.class, () ->
                refreshTokenService.rotateToken(aliceReg.refreshToken(), request1),
                "Replaying old token outside grace period must throw security compromise error"
        );
        assertTrue(refreshTokenRepository.findByFamilyId(familyId).isEmpty(),
                "Entire compromised token family must be permanently wiped from database table");

        // 7. Verify Phone session was NOT affected by PC's replay incident
        Optional<RefreshToken> phoneSession = refreshTokenRepository.findByToken(alicePhone.refreshToken());
        assertTrue(phoneSession.isPresent(), "Phone session must remain active despite PC family compromise");

        // 8. Global Logout
        authService.logoutUser("alice@devlynix.com");
        assertEquals(0, refreshTokenRepository.count(),
                "Global logout must permanently delete all tokens for Alice from the database table");
    }

    @Test
    @DisplayName("COMBINED TEST 2: Discovery, Synergy Scoring, Swiping, Matching, and Chat Sanitization")
    void testDiscoveryMatchAndChatLifecycleCombined() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Register User Alice (Java, React, PostgreSQL)
        authService.register(new RegisterRequest(
                "Alice", "alice@test.com", "Password123!",
                "https://github.com/alice", "AI builder",
                List.of("Java", "React", "PostgreSQL")
        ), request);

        // Register User Bob (Java, Spring Boot, PostgreSQL)
        authService.register(new RegisterRequest(
                "Bob", "bob@test.com", "Password123!",
                "https://github.com/bob", "Backend developer",
                List.of("Java", "Spring Boot", "PostgreSQL")
        ), request);

        User aliceUser = userRepository.findByEmail("alice@test.com").orElseThrow();
        User bobUser = userRepository.findByEmail("bob@test.com").orElseThrow();

        // 1. Self-Swiping is strictly forbidden
        assertThrows(IllegalArgumentException.class, () ->
                matchService.swipe(aliceUser, aliceUser.getId(), Swipe.Direction.LIKE),
                "Users must not be allowed to swipe on themselves"
        );

        // 2. Bob likes Alice
        MatchResponse bobSwipe = matchService.swipe(bobUser, aliceUser.getId(), Swipe.Direction.LIKE);
        assertFalse(bobSwipe.matched(), "Single like should not create a mutual match");

        // 3. Alice inspects incoming requests
        List<ProfileResponse> aliceIncoming = matchService.getIncomingRequests("alice@test.com");
        assertEquals(1, aliceIncoming.size());
        assertEquals("Bob", aliceIncoming.get(0).name());

        // 4. Alice likes Bob back -> MUTUAL MATCH FORMED!
        MatchResponse aliceSwipe = matchService.swipe(aliceUser, bobUser.getId(), Swipe.Direction.LIKE);
        assertTrue(aliceSwipe.matched(), "Mutual like must establish a match");
        assertNotNull(aliceSwipe.id(), "Match ID must be assigned");

        Long matchId = aliceSwipe.id();

        // 5. Incoming requests for Alice should now be empty (since Bob is now an active match)
        assertTrue(matchService.getIncomingRequests("alice@test.com").isEmpty());

        // 6. Chat Messaging: Normal message from Alice
        MessageResponse msg1 = chatService.sendMessage("alice@test.com", matchId, "Hey Bob, let's team up!");
        assertEquals("Hey Bob, let's team up!", msg1.content());
        assertEquals("Alice", msg1.senderName());

        // 7. Security: XSS Payload from Bob -> Must be safely sanitized!
        String maliciousPayload = "<script>alert('Stealing tokens')</script>Awesome! Let's hack!";
        MessageResponse msg2 = chatService.sendMessage("bob@test.com", matchId, maliciousPayload);
        assertEquals("Awesome! Let's hack!", msg2.content(),
                "Malicious script tags must be completely stripped by SanitizationUtil");
        assertFalse(msg2.content().contains("<script>"), "Stored message must never contain script tags");

        // 8. Security: Malicious-only payload results in empty content and must be rejected
        assertThrows(IllegalArgumentException.class, () ->
                chatService.sendMessage("bob@test.com", matchId, "<script>evil()</script>"),
                "Blank or all-tag payloads must throw IllegalArgumentException"
        );

        // 9. Message History and Pagination
        List<MessageResponse> history = chatService.getMessages("alice@test.com", matchId);
        assertEquals(2, history.size(), "Should have 2 messages in conversation");

        // Delta pagination with afterId
        List<MessageResponse> delta = chatService.getMessages("alice@test.com", matchId, msg1.id());
        assertEquals(1, delta.size(), "Delta query with afterId should only return messages after msg1");
        assertEquals(msg2.id(), delta.get(0).id());

        // 10. Mark Chat as Read & Clear Chat
        chatService.markAsRead("alice@test.com", matchId);
        chatService.clearChat("alice@test.com", matchId);
        assertTrue(chatService.getMessages("alice@test.com", matchId).isEmpty(),
                "Clear chat must delete all messages in match");

        // 11. Unmatch
        matchService.unmatch("alice@test.com", matchId);
        assertTrue(matchService.getMatches("alice@test.com").isEmpty(),
                "Unmatching must remove the match between Alice and Bob");
    }

    @Test
    @DisplayName("COMBINED TEST 3: Neon Stale & Revoked Refresh Token Physical Purge")
    void testNeonDatabaseCleanupCombined() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Register user
        authService.register(new RegisterRequest(
                "Charlie", "charlie@test.com", "Password123!",
                "https://github.com/charlie", "DevOps Engineer",
                List.of("Kubernetes", "AWS")
        ), request);

        User charlie = userRepository.findByEmail("charlie@test.com").orElseThrow();

        // Create 3 active tokens for Charlie
        RefreshToken token1 = refreshTokenService.createSession(charlie, request);
        RefreshToken token2 = refreshTokenService.createSession(charlie, request);
        RefreshToken token3 = refreshTokenService.createSession(charlie, request);

        // Simulate token1 as EXPIRED (e.g. 8 days old)
        token1.setExpiryDate(Instant.now().minus(Duration.ofDays(8)));
        refreshTokenRepository.save(token1);

        // Simulate token2 as REVOKED (e.g. from an old session)
        token2.setRevoked(true);
        token2.setRevokedAt(Instant.now().minus(Duration.ofMinutes(5)));
        refreshTokenRepository.save(token2);

        // Token3 remains ACTIVE and VALID

        // Execute Neon database cleanup
        int purgedCount = authService.cleanupTokens();
        assertTrue(purgedCount >= 2, "Cleanup must purge at least the 2 stale/revoked tokens");

        // Verify token1 and token2 are PHYSICALLY DELETED from the database table
        assertTrue(refreshTokenRepository.findByToken(token1.getToken()).isEmpty(),
                "Expired token must be physically deleted from database");
        assertTrue(refreshTokenRepository.findByToken(token2.getToken()).isEmpty(),
                "Revoked token must be physically deleted from database");

        // Verify token3 is STILL IN DATABASE and functional
        Optional<RefreshToken> activeToken = refreshTokenRepository.findByToken(token3.getToken());
        assertTrue(activeToken.isPresent(), "Active valid token must remain untouched in database");
        assertFalse(activeToken.get().isRevoked());
    }
}
