package com.credixa.controller;

import com.credixa.dto.request.*;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and session management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends an OTP for verification")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("Registration successful. Please verify OTP sent to your email."), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns access and refresh tokens")
    public ResponseEntity<ApiResponse<Object>> login(
            @Valid @RequestBody LoginRequestDTO request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        Object response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP sent during registration or password reset")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO request) {
        AuthResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP verified successfully"));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resends a new OTP to the user's email or phone")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequestDTO request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("New OTP sent successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generates a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidates the user's session and blacklists the token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequestDTO request,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.substring(7);
        authService.logout(request, accessToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password reset process by sending an OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user's password using a valid OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
    }
}
