package com.devtinder.controller;

import com.devtinder.dto.request.SwipeRequest;
import com.devtinder.dto.response.DiscoverResponse;
import com.devtinder.dto.response.MatchResponse;
import com.devtinder.service.DiscoverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final DiscoverService discoverService;

    public DiscoverController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @GetMapping
    public List<DiscoverResponse> discover(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String skill
    ) {
        return discoverService.discover(userDetails.getUsername(), skill);
    }

    @PostMapping("/swipe")
    public MatchResponse swipe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SwipeRequest request
    ) {
        return discoverService.swipe(userDetails.getUsername(), request.targetUserId(), request.direction());
    }
}
