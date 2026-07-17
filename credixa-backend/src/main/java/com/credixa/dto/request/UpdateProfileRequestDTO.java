package com.credixa.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileRequestDTO {
    @Size(min = 2, max = 80, message = "First name must be between 2 and 80 characters")
    private String firstName;

    @Size(min = 2, max = 80, message = "Last name must be between 2 and 80 characters")
    private String lastName;

    private String address;
    private String city;
    private String state;

    @Size(min = 6, max = 6, message = "Pincode must be 6 digits")
    private String pincode;
}
