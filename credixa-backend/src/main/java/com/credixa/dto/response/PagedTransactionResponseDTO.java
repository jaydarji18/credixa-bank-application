package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedTransactionResponseDTO {
    private List<TransactionResponseDTO> content;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean last;
}
