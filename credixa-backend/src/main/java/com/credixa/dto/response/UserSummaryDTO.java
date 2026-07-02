package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private String userCode;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String kycStatus;
    private boolean spinSet;
}
