package com.credixa.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequestDTO {
    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotBlank(message = "IFSC code is required")
    private String ifscCode;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private String phone;
}
