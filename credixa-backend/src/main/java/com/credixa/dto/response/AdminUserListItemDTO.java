package com.credixa.dto.response;

import com.credixa.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListItemDTO {
    private String userCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private User.UserStatus status;
    private User.KycStatus kycStatus;
    private BigDecimal totalBalance;
    private LocalDateTime createdAt;
}
