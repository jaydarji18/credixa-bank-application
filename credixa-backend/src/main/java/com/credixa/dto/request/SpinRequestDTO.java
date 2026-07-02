package com.credixa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpinRequestDTO {
    
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be exactly 6 digits")
    private String spin;
}
