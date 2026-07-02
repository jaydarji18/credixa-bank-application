package com.credixa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBeneficiaryRequestDTO {

    @NotBlank(message = "Beneficiary name is required")
    @Size(min = 2, max = 150, message = "Beneficiary name must be between 2 and 150 characters")
    private String beneficiaryName;

    @NotBlank(message = "Account number is required")
    @Size(min = 9, max = 20, message = "Account number must be between 9 and 20 characters")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z0-9]{11}$", message = "IFSC code must be exactly 11 alphanumeric characters")
    private String ifscCode;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @Size(max = 80, message = "Nickname must not exceed 80 characters")
    private String nickname;
}
