package com.credixa.dto.request;

import com.credixa.entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {
    
    @NotNull(message = "Status is required")
    private User.UserStatus status;

    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}
