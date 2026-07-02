package com.credixa.controller;

import com.credixa.dto.request.BranchRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.BranchResponseDTO;
import com.credixa.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/branches")
@RequiredArgsConstructor
@Tag(name = "Admin Branch Management", description = "Endpoints for managing bank branches")
public class AdminBranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Get all branches")
    public ResponseEntity<ApiResponse<List<BranchResponseDTO>>> getAllBranches() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAllBranches(), "Branches retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by id")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranchById(id), "Branch retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new branch")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> createBranch(@Valid @RequestBody BranchRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(branchService.createBranch(request), "Branch created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing branch")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateBranch(id, request), "Branch updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a branch")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Branch deleted successfully"));
    }
}
