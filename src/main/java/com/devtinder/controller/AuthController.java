package com.devtinder.controller;

import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (userDetails != null) {
            return authService.refreshToken(userDetails.getUsername());
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authService.refreshTokenFromToken(authHeader.substring(7));
        }
        throw new IllegalArgumentException("Authentication required to refresh token");
    }
}
