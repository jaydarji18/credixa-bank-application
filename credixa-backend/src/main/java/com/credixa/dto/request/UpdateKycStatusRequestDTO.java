package com.credixa.dto.request;

import com.credixa.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKycStatusRequestDTO {

    @NotNull(message = "KYC status is required")
    private User.KycStatus kycStatus;

    private String remarks;
}
