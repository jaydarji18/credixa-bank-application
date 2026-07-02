package com.credixa.controller;

import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.NotificationListResponseDTO;
import com.credixa.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponseDTO>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        NotificationListResponseDTO response = notificationService.getUserNotifications(getCurrentUserCode(), isRead, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved successfully"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        int updatedCount = notificationService.markAllAsRead(getCurrentUserCode());
        return ResponseEntity.ok(ApiResponse.success(updatedCount, "All notifications marked as read"));
    }
}
