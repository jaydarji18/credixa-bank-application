package com.credixa.service;

import com.credixa.dto.response.NotificationListResponseDTO;
import com.credixa.dto.response.NotificationResponseDTO;
import com.credixa.entity.Notification;
import com.credixa.entity.User;
import com.credixa.exception.ForbiddenException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.NotificationRepository;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationListResponseDTO getUserNotifications(String userCode, Boolean isRead, Pageable pageable) {
        User user = findUserByCode(userCode);
        
        Page<Notification> notificationPage;
        if (isRead != null) {
            notificationPage = notificationRepository.findByUserAndIsRead(user, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByUser(user, pageable);
        }

        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);

        List<NotificationResponseDTO> notifications = notificationPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return NotificationListResponseDTO.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .totalItems(notificationPage.getTotalElements())
                .build();
    }

    @Transactional
    public void markAsRead(Long id, String userCode) {
        User user = findUserByCode(userCode);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to access this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("Notification {} marked as read for user {}", id, userCode);
        }
    }

    @Transactional
    public int markAllAsRead(String userCode) {
        User user = findUserByCode(userCode);
        int updatedCount = notificationRepository.markAllAsRead(user);
        log.info("Marked {} notifications as read for user {}", updatedCount, userCode);
        return updatedCount;
    }

    @Async
    @Transactional
    public void sendTransactionNotification(Long userId, String title, String body, Notification.NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .notificationType(type)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        
        // Also send email
        sendNotificationEmail(user, title, body);

        log.info("Notification saved and sent to user {}: {}", userId, body);
    }

    @Async
    @Transactional
    public void sendGlobalNotification(User user, String title, String body, Notification.NotificationType type) {
        // Re-fetch user or ensure it's attached to the current session if needed
        // But since we have the user object, let's just use it. 
        // If it was detached, we might need to merge or re-fetch.
        User managedUser = userRepository.findById(user.getId())
                .orElse(user);

        Notification notification = Notification.builder()
                .user(managedUser)
                .title(title)
                .body(body)
                .notificationType(type)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        sendNotificationEmail(managedUser, title, body);
        log.info("Global notification sent to user {}: {}", managedUser.getId(), title);
    }

    private void sendNotificationEmail(User user, String title, String body) {
        String htmlBody = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
                "  <div style='max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "    <h2 style='color: #004a99; text-align: center;'>Credixa Pro</h2>" +
                "    <h3 style='color: #333;'>" + title + "</h3>" +
                "    <hr style='border: 0; border-top: 1px solid #eee;'>" +
                "    <p>Dear " + user.getFirstName() + ",</p>" +
                "    <p>" + body + "</p>" +
                "    <p>If you have any questions, please contact our 24/7 support.</p>" +
                "    <br>" +
                "    <p style='font-size: 12px; color: #777; text-align: center;'>&copy; 2026 Credixa Pro Banking. All rights reserved.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";

        log.info("Triggering notification email for user {} at {}", user.getId(), user.getEmail());
        emailService.sendEmail(user.getEmail(), "Credixa Pro: " + title, htmlBody, true);
    }

    @Async
    public void sendOtpEmail(String email, String otp, String purpose) {
        String subject = "Credixa Pro - " + purpose;
        String body = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
                "  <div style='max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "    <h2 style='color: #004a99; text-align: center;'>Credixa Pro</h2>" +
                "    <hr style='border: 0; border-top: 1px solid #eee;'>" +
                "    <p>Hello,</p>" +
                "    <p>Your OTP for <strong>" + purpose + "</strong> is:</p>" +
                "    <div style='text-align: center; margin: 30px 0;'>" +
                "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #004a99; background: #eef4ff; padding: 10px 20px; border-radius: 5px;'>" + otp + "</span>" +
                "    </div>" +
                "    <p>This OTP is valid for 15 minutes. Please do not share it with anyone.</p>" +
                "    <p>If you didn't request this, please ignore this email or contact support.</p>" +
                "    <br>" +
                "    <p style='font-size: 12px; color: #777; text-align: center;'>&copy; 2026 Credixa Pro Banking. All rights reserved.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";

        emailService.sendEmail(email, subject, body, true);
    }

    @Async
    public void sendOtpSms(String phone, String otp, String purpose) {
        String message = "Credixa Pro: Your OTP for " + purpose + " is " + otp + ". Valid for 15 minutes.";
        smsService.sendSms(phone, message);
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with code: " + userCode));
    }

    private NotificationResponseDTO mapToDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.isRead())
                .notificationType(n.getNotificationType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
