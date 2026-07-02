package com.credixa.controller;

import com.credixa.dto.request.AddBeneficiaryRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.BeneficiaryResponseDTO;
import com.credixa.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Endpoints for managing transfer recipients")
public class BeneficiaryController extends BaseController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryResponseDTO>>> getBeneficiaries() {
        List<BeneficiaryResponseDTO> beneficiaries = beneficiaryService.getBeneficiaries(getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.success(beneficiaries, "Beneficiaries retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BeneficiaryResponseDTO>> getBeneficiary(@PathVariable Long id) {
        BeneficiaryResponseDTO beneficiary = beneficiaryService.getBeneficiaryById(id, getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.success(beneficiary, "Beneficiary retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryResponseDTO>> addBeneficiary(@Valid @RequestBody AddBeneficiaryRequestDTO request) {
        BeneficiaryResponseDTO beneficiary = beneficiaryService.addBeneficiary(getCurrentUserCode(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(beneficiary, "Beneficiary added successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete beneficiary", description = "Removes a beneficiary from the user's list")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id, getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Beneficiary deleted successfully"));
    }
}
