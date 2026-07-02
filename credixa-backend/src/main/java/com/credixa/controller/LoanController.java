package com.credixa.controller;

import com.credixa.dto.request.LoanApplicationRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.dto.response.LoanResponseDTO;
import com.credixa.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Endpoints for loan eligibility checks and applications")
public class LoanController extends BaseController {

    private final LoanService loanService;

    @GetMapping("/products")
    @Operation(summary = "Get loan products", description = "Lists all available loan types (Personal, Home, Car, etc.) with interest rates")
    public ResponseEntity<ApiResponse<List<LoanProductResponseDTO>>> getLoanProducts() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoanProducts(), "Loan products retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanResponseDTO>>> getUserLoans() {
        return ResponseEntity.ok(ApiResponse.success(loanService.getUserLoans(getCurrentUserCode()), "User loans retrieved successfully"));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> getLoanDetail(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoanById(loanId, getCurrentUserCode()), "Loan details retrieved successfully"));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> applyForLoan(@Valid @RequestBody LoanApplicationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(loanService.applyForLoan(getCurrentUserCode(), request), "Loan application submitted successfully"));
    }
}
