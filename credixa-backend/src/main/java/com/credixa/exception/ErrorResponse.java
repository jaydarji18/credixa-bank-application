package com.credixa.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<ValidationError> details;

    @Data
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}
