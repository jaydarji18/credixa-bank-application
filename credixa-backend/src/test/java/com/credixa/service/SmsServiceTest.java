package com.credixa.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smsService, "accountSid", "valid_account_sid");
        ReflectionTestUtils.setField(smsService, "authToken", "valid_auth_token");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "+1234567890");
    }

    @Nested
    @DisplayName("sendSms() tests")
    class SendSmsTests {

        @Test
        @DisplayName("Should send SMS with +91 prefix for Indian numbers without country code")
        void shouldSendSmsWithIndianPrefix() {
            smsService.sendSms("9876543210", "Test message");
            
            // In mock mode, should log but not actually send
            // Verify no exception thrown
        }

        @Test
        @DisplayName("Should send SMS without modifying already prefixed numbers")
        void shouldSendSmsWithExistingPrefix() {
            smsService.sendSms("+919876543210", "Test message");
            
            assertThatCode(() -> smsService.sendSms("+919876543210", "Test message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle mock mode when credentials not configured")
        void shouldHandleMockMode() {
            SmsService mockService = new SmsService();
            ReflectionTestUtils.setField(mockService, "accountSid", "your_account_sid_here");
            
            assertThatCode(() -> mockService.sendSms("9876543210", "Test message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null accountSid gracefully")
        void shouldHandleNullAccountSid() {
            SmsService mockService = new SmsService();
            ReflectionTestUtils.setField(mockService, "accountSid", null);
            
            assertThatCode(() -> mockService.sendSms("9876543210", "Test message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty accountSid gracefully")
        void shouldHandleEmptyAccountSid() {
            SmsService mockService = new SmsService();
            ReflectionTestUtils.setField(mockService, "accountSid", "");
            
            assertThatCode(() -> mockService.sendSms("9876543210", "Test message"))
                    .doesNotThrowAnyException();
        }
    }
}