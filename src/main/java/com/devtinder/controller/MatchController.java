package com.devtinder.controller;

import com.devtinder.dto.response.MatchResponse;
import com.devtinder.service.MatchService;
import com.devtinder.dto.response.ProfileResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> matches(@AuthenticationPrincipal UserDetails userDetails) {
        return matchService.getMatches(userDetails.getUsername());
    }

    @GetMapping("/requests")
    public List<ProfileResponse> requests(@AuthenticationPrincipal UserDetails userDetails) {
        return matchService.getIncomingRequests(userDetails.getUsername());
    }

    @DeleteMapping("/{matchId}")
    public void unmatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long matchId
    ) {
        matchService.unmatch(userDetails.getUsername(), matchId);
    }
}

