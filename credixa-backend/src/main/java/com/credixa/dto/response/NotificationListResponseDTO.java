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
public class NotificationListResponseDTO {
    private List<NotificationResponseDTO> notifications;
    private long unreadCount;
    private long totalItems;
}
