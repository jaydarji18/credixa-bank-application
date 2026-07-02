package com.credixa.dto.response;

import com.credixa.entity.Beneficiary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponseDTO {
    private Long id;
    private String beneficiaryName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String nickname;
    private boolean isVerified;
    private Beneficiary.BeneficiaryStatus status;
    private LocalDateTime createdAt;
}
