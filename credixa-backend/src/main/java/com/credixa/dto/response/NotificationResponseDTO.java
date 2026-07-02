package com.credixa.dto.response;

import com.credixa.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private String title;
    private String body;
    private boolean isRead;
    private Notification.NotificationType notificationType;
    private LocalDateTime createdAt;
}
