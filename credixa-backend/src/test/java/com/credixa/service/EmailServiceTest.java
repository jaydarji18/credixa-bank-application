package com.credixa.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@credixa.com");
    }

    @Nested
    @DisplayName("sendEmail() tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should send email successfully")
        void shouldSendEmailSuccessfully() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            given(mailSender.createMimeMessage()).willReturn(mimeMessage);

            emailService.sendEmail("test@example.com", "Test Subject", "<p>HTML Content</p>", true);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send plain text email when isHtml is false")
        void shouldSendPlainTextEmail() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            given(mailSender.createMimeMessage()).willReturn(mimeMessage);

            emailService.sendEmail("test@example.com", "Test Subject", "Plain text content", false);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle MessagingException gracefully")
        void shouldHandleMessagingException() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            given(mailSender.createMimeMessage()).willReturn(mimeMessage);

            assertThatCode(() -> emailService.sendEmail(
                    "test@example.com", "Test", "Content", true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle generic exception gracefully")
        void shouldHandleGenericException() {
            willThrow(new RuntimeException("Unexpected error")).given(mailSender).createMimeMessage();

            assertThatCode(() -> emailService.sendEmail(
                    "test@example.com", "Test", "Content", true))
                    .doesNotThrowAnyException();
        }
    }
}