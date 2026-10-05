package com.devtinder.service;

import com.devtinder.dto.response.MatchResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.entity.Swipe;
import com.devtinder.entity.User;
import com.devtinder.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserRepository userRepository;

    private User userAlice;
    private User userBob;

    @BeforeEach
    void setUp() {
        userAlice = createUser("Alice", "alice@test.com");
        userBob = createUser("Bob", "bob@test.com");
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("hash123");
        user.setGithubUrl("https://github.com/" + name.toLowerCase());
        user.setBio("Dev bio");
        user.setLookingFor("Hackathon team");
        user.setLocation("SF");
        return userRepository.save(user);
    }

    @Test
    void testIncomingRequestsFlow() {
        // Initially, Alice has no incoming requests
        List<ProfileResponse> requestsBefore = matchService.getIncomingRequests(userAlice.getEmail());
        assertTrue(requestsBefore.isEmpty());

        // Bob likes Alice
        matchService.swipe(userBob, userAlice.getId(), Swipe.Direction.LIKE);

        // Now Alice should see Bob in incoming requests
        List<ProfileResponse> requestsAfter = matchService.getIncomingRequests(userAlice.getEmail());
        assertEquals(1, requestsAfter.size());
        assertEquals(userBob.getId(), requestsAfter.get(0).id());
        assertEquals("Bob", requestsAfter.get(0).name());

        // Bob should NOT see Alice in incoming requests (Alice hasn't liked Bob)
        List<ProfileResponse> bobRequests = matchService.getIncomingRequests(userBob.getEmail());
        assertTrue(bobRequests.isEmpty());

        // Alice accepts & likes Bob back -> Mutual match formed
        MatchResponse matchResponse = matchService.swipe(userAlice, userBob.getId(), Swipe.Direction.LIKE);
        assertTrue(matchResponse.matched());

        // Now Alice's incoming requests should be empty again (they are matched!)
        List<ProfileResponse> requestsFinal = matchService.getIncomingRequests(userAlice.getEmail());
        assertTrue(requestsFinal.isEmpty());
    }

    @Test
    void testDecliningIncomingRequest() {
        // Bob likes Alice
        matchService.swipe(userBob, userAlice.getId(), Swipe.Direction.LIKE);

        // Alice declines by swiping PASS on Bob
        MatchResponse passResponse = matchService.swipe(userAlice, userBob.getId(), Swipe.Direction.PASS);
        assertFalse(passResponse.matched());

        // Alice's incoming requests should be empty
        List<ProfileResponse> requests = matchService.getIncomingRequests(userAlice.getEmail());
        assertTrue(requests.isEmpty());
    }

    @Autowired
    private DiscoverService discoverService;

    @Test
    void testUnmatch() {
        // Form a match between Alice and Bob
        matchService.swipe(userBob, userAlice.getId(), Swipe.Direction.LIKE);
        MatchResponse matchResponse = matchService.swipe(userAlice, userBob.getId(), Swipe.Direction.LIKE);
        assertTrue(matchResponse.matched());

        List<MatchResponse> matchesBefore = matchService.getMatches(userAlice.getEmail());
        assertEquals(1, matchesBefore.size());

        // Unmatch
        matchService.unmatch(userAlice.getEmail(), matchResponse.id());

        // Should have 0 matches now
        List<MatchResponse> matchesAfter = matchService.getMatches(userAlice.getEmail());
        assertTrue(matchesAfter.isEmpty());
    }

    @Test
    void testResetPasses() {
        // Alice passes on Bob
        matchService.swipe(userAlice, userBob.getId(), Swipe.Direction.PASS);

        // Discover for Alice should not show Bob
        var discoverBefore = discoverService.discover(userAlice.getEmail(), null);
        assertTrue(discoverBefore.stream().noneMatch(d -> d.profile().id().equals(userBob.getId())));

        // Reset passes
        discoverService.resetPasses(userAlice.getEmail());

        // Now Bob should be discoverable again!
        var discoverAfter = discoverService.discover(userAlice.getEmail(), null);
        assertTrue(discoverAfter.stream().anyMatch(d -> d.profile().id().equals(userBob.getId())));
    }
}
