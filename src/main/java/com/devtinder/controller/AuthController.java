package com.devtinder.controller;

import com.devtinder.dto.request.LoginRequest;
import com.devtinder.dto.request.RefreshRequest;
import com.devtinder.dto.request.RegisterRequest;
import com.devtinder.dto.response.AuthResponse;
import com.devtinder.dto.response.SessionResponse;
import com.devtinder.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody(required = false) RefreshRequest request,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken,
            HttpServletRequest httpRequest
    ) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : headerToken;

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        return authService.refreshToken(token.trim(), httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestBody(required = false) RefreshRequest request,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : headerToken;

        if (token != null && !token.isBlank()) {
            authService.logout(token.trim());
        } else if (userDetails != null) {
            authService.logoutUser(userDetails.getUsername());
        }
    }

    @GetMapping("/sessions")
    public List<SessionResponse> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken
    ) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return authService.getSessions(userDetails.getUsername(), headerToken);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        authService.logoutUser(userDetails.getUsername());
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        authService.terminateSession(userDetails.getUsername(), id);
    }

    @PostMapping("/sessions/terminate-others")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateOtherSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) RefreshRequest request,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken
    ) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : headerToken;
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Current refresh token is required");
        }
        authService.terminateOtherSessions(userDetails.getUsername(), token.trim());
    }
}
