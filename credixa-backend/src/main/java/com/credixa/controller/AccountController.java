package com.credixa.controller;

import com.credixa.dto.request.CreateAccountRequestDTO;
import com.credixa.dto.response.AccountResponseDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.DashboardSummaryDTO;
import com.credixa.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Endpoints for bank account management and lifecycle")
public class AccountController extends BaseController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all user accounts", description = "Retrieves a list of all accounts associated with the authenticated user")
    public ResponseEntity<ApiResponse<List<AccountResponseDTO>>> getUserAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getUserAccounts(getCurrentUserCode()), "Accounts fetched successfully"));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", description = "Retrieves specific account details by its unique identifier")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountById(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountById(accountId, getCurrentUserCode()), "Account details fetched successfully"));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Retrieves a summary of all user balances and financial health indicators")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getDashboardSummary(getCurrentUserCode()), "Dashboard summary fetched successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a SAVINGS, CURRENT or FD account for the authenticated user")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> createAccount(@Valid @RequestBody CreateAccountRequestDTO request) {
        AccountResponseDTO response = accountService.createAccount(getCurrentUserCode(), request);
        return new ResponseEntity<>(ApiResponse.success(response, "Account created successfully"), HttpStatus.CREATED);
    }
}
