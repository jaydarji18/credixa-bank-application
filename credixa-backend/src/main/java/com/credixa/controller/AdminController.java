package com.credixa.controller;

import com.credixa.dto.request.UpdateKycStatusRequestDTO;
import com.credixa.dto.request.UpdateUserStatusRequestDTO;
import com.credixa.dto.response.*;
import com.credixa.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints for platform management and auditing")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Get platform statistics", description = "Retrieves high-level platform stats including user counts and transaction volume")
    public ResponseEntity<ApiResponse<AdminStatsResponseDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAdminStats(), "Admin stats retrieved successfully"));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Retrieves a paginated list of users with advanced filtering by status and KYC")
    public ResponseEntity<ApiResponse<Page<AdminUserListItemDTO>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers(search, status, kycStatus, pageable), "Users retrieved successfully"));
    }

    @GetMapping("/users/{userCode}")
    @Operation(summary = "Get user details", description = "Retrieves full profile, account list, and loan summary for a specific user")
    public ResponseEntity<ApiResponse<AdminUserDetailDTO>> getUserDetail(@PathVariable String userCode) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserDetail(userCode), "User detail retrieved successfully"));
    }

    @PatchMapping("/users/{userCode}/status")
    @Operation(summary = "Update user account status", description = "Changes a user's status (ACTIVE/BLOCKED/SUSPENDED) with audit reason")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String userCode,
            @Valid @RequestBody UpdateUserStatusRequestDTO request) {
        
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.updateUserStatus(userCode, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "User status updated successfully"));
    }

    @PatchMapping("/users/{userCode}/kyc")
    @Operation(summary = "Update KYC status", description = "Approves or rejects a user's KYC verification request")
    public ResponseEntity<ApiResponse<Void>> updateKycStatus(
            @PathVariable String userCode,
            @Valid @RequestBody UpdateKycStatusRequestDTO request) {
        
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.updateKycStatus(userCode, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "KYC status updated successfully"));
    }

    @PutMapping("/users/{userCode}")
    @Operation(summary = "Update user details", description = "Updates basic user information like name, email, and phone")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable String userCode,
            @Valid @RequestBody com.credixa.dto.request.AdminUpdateUserRequestDTO request) {
        
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.updateUser(userCode, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "User details updated successfully"));
    }

    @DeleteMapping("/users/{userCode}")
    @Operation(summary = "Soft delete user", description = "Deactivates a user account and marks it as deleted")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userCode) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.deleteUser(userCode, adminEmail);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "User deleted successfully"));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Audit transactions", description = "Performs deep auditing of platform-wide transactions with varied filters")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> getTransactions(
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("initiatedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(adminService.getAdminTransactions(userCode, type, status, minAmount, maxAmount, fromDate, toDate, pageable), "Transactions retrieved successfully"));
    }

    @GetMapping("/loans/pending")
    @Operation(summary = "List pending loan applications", description = "Retrieves all loans with APPLIED status for review")
    public ResponseEntity<ApiResponse<List<LoanResponseDTO>>> getPendingLoans() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPendingLoans(), "Pending loans retrieved successfully"));
    }

    @PostMapping("/loans/{loanId}/approve")
    @Operation(summary = "Approve loan application", description = "Sanctions and disburses loan amount to user account")
    public ResponseEntity<ApiResponse<Void>> approveLoan(@PathVariable Long loanId) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.approveLoan(loanId, adminEmail);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Loan approved and disbursed successfully"));
    }
}
