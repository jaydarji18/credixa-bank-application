package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminUserDetailDTO extends AdminUserListItemDTO {
    private List<AccountResponseDTO> accounts;
    private List<LoanResponseDTO> loans;
    private LocalDateTime lastLoginAt;
    private String kycRemarks;
}
