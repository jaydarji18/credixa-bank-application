package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.ByteArrayResource;

@Data
@AllArgsConstructor
public class StatementFileDTO {
    private ByteArrayResource resource;
    private String filename;
}
