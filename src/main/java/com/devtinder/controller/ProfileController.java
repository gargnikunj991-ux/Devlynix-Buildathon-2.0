package com.devtinder.controller;

import com.devtinder.dto.request.UpdateProfileRequest;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal UserDetails userDetails) {
        return profileService.getProfile(userDetails.getUsername());
    }

    @PutMapping("/me")
    public ProfileResponse update(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(userDetails.getUsername(), request);
    }
}
