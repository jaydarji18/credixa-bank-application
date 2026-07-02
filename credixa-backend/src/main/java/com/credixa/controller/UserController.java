package com.credixa.controller;

import com.credixa.dto.request.ChangePasswordRequestDTO;
import com.credixa.dto.request.UpdateProfileRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.UserProfileResponseDTO;
import com.credixa.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user profile management and security")
public class UserController extends BaseController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user profile", description = "Retrieves the full profile details of the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(getCurrentUserCode()), "Profile fetched successfully"));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(getCurrentUserCode(), request), "Profile updated successfully"));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        userService.changePassword(getCurrentUserCode(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/me/photo")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        String url = userService.uploadProfilePhoto(getCurrentUserCode(), file);
        return ResponseEntity.ok(ApiResponse.success(url, "Profile photo uploaded successfully"));
    }
    @PostMapping("/me/kyc/submit")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Submit KYC", description = "Submits the user's KYC details for verification")
    public ResponseEntity<ApiResponse<Void>> submitKyc() {
        userService.submitKyc(getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.success("KYC submitted successfully. Status: PENDING"));
    }

    @PostMapping("/me/kyc/verify")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Verify KYC (Simulated)", description = "Simulates an administrative approval of the user's KYC")
    public ResponseEntity<ApiResponse<Void>> verifyKyc() {
        userService.simulateKycVerification(getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.success("KYC verified successfully. Account is now ACTIVE."));
    }

    @PostMapping("/me/spin")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Set sPin", description = "Sets a 6-digit secret PIN for secure transactions")
    public ResponseEntity<ApiResponse<Void>> setSpin(@Valid @RequestBody com.credixa.dto.request.SpinRequestDTO request) {
        userService.setSpin(getCurrentUserCode(), request.getSpin());
        return ResponseEntity.ok(ApiResponse.success("sPin set successfully"));
    }

    @PostMapping("/me/spin/verify")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Verify sPin", description = "Verifies the user's 6-digit secret PIN")
    public ResponseEntity<ApiResponse<Void>> verifySpin(@Valid @RequestBody com.credixa.dto.request.SpinRequestDTO request) {
        userService.verifySpin(getCurrentUserCode(), request.getSpin());
        return ResponseEntity.ok(ApiResponse.success("sPin verified successfully"));
    }
}

