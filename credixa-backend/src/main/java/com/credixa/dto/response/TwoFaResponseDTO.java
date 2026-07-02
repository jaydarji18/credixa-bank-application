package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFaResponseDTO {
    private boolean success;
    @Builder.Default
    private boolean twoFaRequired = true;
    private String sessionToken;
    private String message;
}
