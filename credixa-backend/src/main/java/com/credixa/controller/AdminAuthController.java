package com.credixa.controller;

import com.credixa.dto.request.AdminRegisterRequestDTO;
import com.credixa.dto.request.LoginRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Endpoints for administrative staff login")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticates administrative staff and returns access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = adminAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Admin Support Application", description = "Allows internal users to apply for support agent administrative access")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AdminRegisterRequestDTO request) {
        adminAuthService.register(request);
        return new ResponseEntity<>(ApiResponse.success("Admin registration submitted for approval"), HttpStatus.CREATED);
    }
}
