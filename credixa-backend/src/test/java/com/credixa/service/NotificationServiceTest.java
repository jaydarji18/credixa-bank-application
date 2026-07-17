package com.credixa.service;

import com.credixa.dto.response.NotificationListResponseDTO;
import com.credixa.dto.response.NotificationResponseDTO;
import com.credixa.entity.Notification;
import com.credixa.entity.User;
import com.credixa.exception.ForbiddenException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.NotificationRepository;
import com.credixa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .email("john@example.com")
                .build();
    }

    @Nested
    @DisplayName("getUserNotifications() tests")
    class GetUserNotificationsTests {

        @Test
        @DisplayName("Should return all notifications when isRead is null")
        void shouldReturnAllNotifications() {
            Notification notification = Notification.builder()
                    .id(1L)
                    .user(testUser)
                    .title("Test")
                    .body("Body")
                    .isRead(false)
                    .notificationType(Notification.NotificationType.LOGIN_ALERT)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Notification> page = new PageImpl<>(List.of(notification));

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findByUser(eq(testUser), any()))
                    .willReturn(page);
            given(notificationRepository.countByUserAndIsReadFalse(testUser)).willReturn(1L);

            NotificationListResponseDTO response = notificationService.getUserNotifications(
                    "USR00001", null, Pageable.ofSize(10));

            assertThat(response).isNotNull();
            assertThat(response.getNotifications()).hasSize(1);
            assertThat(response.getUnreadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return only read notifications when isRead is true")
        void shouldReturnReadNotifications() {
            Notification readNotification = Notification.builder()
                    .id(1L)
                    .user(testUser)
                    .title("Read")
                    .isRead(true)
                    .build();

            Page<Notification> page = new PageImpl<>(List.of(readNotification));

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findByUserAndIsRead(eq(testUser), eq(true), any()))
                    .willReturn(page);
            given(notificationRepository.countByUserAndIsReadFalse(testUser)).willReturn(0L);

            NotificationListResponseDTO response = notificationService.getUserNotifications(
                    "USR00001", true, Pageable.ofSize(10));

            assertThat(response).isNotNull();
            assertThat(response.getNotifications()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getUserNotifications(
                    "USR99999", null, Pageable.ofSize(10)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAsRead() tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkAsRead() {
            Notification notification = Notification.builder()
                    .id(1L)
                    .user(testUser)
                    .isRead(false)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            notificationService.markAsRead(1L, "USR00001");

            verify(notificationRepository).save(argThat(n -> n.isRead() == true));
        }

        @Test
        @DisplayName("Should not update when already read")
        void shouldNotUpdateWhenAlreadyRead() {
            Notification notification = Notification.builder()
                    .id(1L)
                    .user(testUser)
                    .isRead(true)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            notificationService.markAsRead(1L, "USR00001");

            // Should not call save since already read
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when notification not found")
        void shouldThrowExceptionWhenNotificationNotFound() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(1L, "USR00001"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user not owner of notification")
        void shouldThrowExceptionWhenUserNotOwner() {
            User otherUser = User.builder().id(2L).userCode("USR00002").build();
            Notification notification = Notification.builder()
                    .id(1L)
                    .user(otherUser)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(1L, "USR00001"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead() tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all notifications as read")
        void shouldMarkAllAsRead() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(notificationRepository.markAllAsRead(testUser)).willReturn(5);

            int count = notificationService.markAllAsRead("USR00001");

            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("sendTransactionNotification() tests")
    class SendTransactionNotificationTests {

        @Test
        @DisplayName("Should send notification successfully")
        void shouldSendNotification() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(i -> i.getArgument(0));

            notificationService.sendTransactionNotification(1L, "Title", "Body", Notification.NotificationType.LOGIN_ALERT);

            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should handle non-existent user gracefully")
        void shouldHandleNonExistentUser() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.sendTransactionNotification(
                    999L, "Title", "Body", Notification.NotificationType.LOGIN_ALERT))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("sendGlobalNotification() tests")
    class SendGlobalNotificationTests {

        @Test
        @DisplayName("Should send global notification successfully")
        void shouldSendGlobalNotification() {
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(i -> i.getArgument(0));

            notificationService.sendGlobalNotification(testUser, "Title", "Body", Notification.NotificationType.KYC_APPROVED);

            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("sendOtpEmail() tests")
    class SendOtpEmailTests {

        @Test
        @DisplayName("Should trigger email service for OTP")
        void shouldTriggerEmailService() {
            notificationService.sendOtpEmail("test@example.com", "123456", "Test Purpose");

            verify(emailService).sendEmail(anyString(), anyString(), anyString(), eq(true));
        }
    }

    @Nested
    @DisplayName("sendOtpSms() tests")
    class SendOtpSmsTests {

        @Test
        @DisplayName("Should trigger SMS service for OTP")
        void shouldTriggerSmsService() {
            notificationService.sendOtpSms("9876543210", "123456", "Test Purpose");

            verify(smsService).sendSms(anyString(), anyString());
        }
    }
}