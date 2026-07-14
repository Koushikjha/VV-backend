package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.SendOtpRequest;
import com.example.auth.dto.VerifyOtpRequest;
import com.example.auth.service.AuthService;
import com.example.auth.service.OnlineUserTracker;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String JWT_COOKIE_NAME = "JWT_TOKEN";

    private final AuthService authService;
    private final OnlineUserTracker onlineUserTracker;

    /**
     * Request an OTP.
     *
     * Prefer one request format rather than supporting JSON and form data
     * in the same endpoint unless form submission is genuinely required.
     */
    @PostMapping(
            value = "/send-otp",
            consumes = "application/json"
    )
    public ResponseEntity<Void> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        authService.sendOtp(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verify an OTP and create the authentication cookie.
     */
    @PostMapping(
            value = "/verify-otp",
            consumes = "application/json"
    )
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.verifyOtp(request);

        ResponseCookie accessCookie = createAccessTokenCookie(
                auth.getAccessToken(),
                Duration.ofSeconds(auth.getExpiresIn())
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        return ResponseEntity.ok(auth);
    }

    /**
     * Refresh authentication.
     *
     * This version assumes the refresh token is supplied in the body.
     * A secure HttpOnly refresh-token cookie is generally safer for a
     * browser application.
     */
    @PostMapping(
            value = "/refresh",
            consumes = "application/json"
    )
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.refresh(request);

        ResponseCookie accessCookie = createAccessTokenCookie(
                auth.getAccessToken(),
                Duration.ofSeconds(auth.getExpiresIn())
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        return ResponseEntity.ok(auth);
    }

    /**
     * Logout and expire the browser cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {

        if (userDetails != null) {
            String phone = userDetails.getUsername();

            authService.logout(phone);
            onlineUserTracker.userLoggedOut(phone);
        }

        ResponseCookie deletedCookie = ResponseCookie
                .from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                deletedCookie.toString()
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseCookie createAccessTokenCookie(
            String accessToken,
            Duration maxAge) {

        return ResponseCookie
                .from(JWT_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}