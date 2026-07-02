package com.credixa.controller;

import com.credixa.dto.request.LoanProductRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loan-products")
@RequiredArgsConstructor
@Tag(name = "Admin Loan Product Management", description = "Endpoints for managing loan products")
public class AdminLoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    @Operation(summary = "Get all loan products")
    public ResponseEntity<ApiResponse<List<LoanProductResponseDTO>>> getAllLoanProducts() {
        return ResponseEntity.ok(ApiResponse.success(loanProductService.getAllLoanProducts(), "Loan products retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan product by id")
    public ResponseEntity<ApiResponse<LoanProductResponseDTO>> getLoanProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanProductService.getLoanProductById(id), "Loan product retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new loan product")
    public ResponseEntity<ApiResponse<LoanProductResponseDTO>> createLoanProduct(@Valid @RequestBody LoanProductRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(loanProductService.createLoanProduct(request), "Loan product created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing loan product")
    public ResponseEntity<ApiResponse<LoanProductResponseDTO>> updateLoanProduct(@PathVariable Long id, @Valid @RequestBody LoanProductRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(loanProductService.updateLoanProduct(id, request), "Loan product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a loan product")
    public ResponseEntity<ApiResponse<Void>> deleteLoanProduct(@PathVariable Long id) {
        loanProductService.deleteLoanProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Loan product deleted successfully"));
    }
}
