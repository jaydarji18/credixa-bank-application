package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponseDTO {
    private Long id;
    private String branchName;
    private String branchCode;
    private String ifscCode;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
}
