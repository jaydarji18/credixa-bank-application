package com.credixa.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        if ("your_account_sid_here".equals(accountSid) || accountSid == null || accountSid.isEmpty()) {
            log.warn("Twilio credentials not configured. SMS service will not send real messages.");
            return;
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized with Account SID: {}", accountSid);
    }

    @Async
    public void sendSms(String phone, String messageContent) {
        if ("your_account_sid_here".equals(accountSid) || accountSid == null || accountSid.isEmpty()) {
            log.info("MOCK SMS to {}: {}", phone, messageContent);
            return;
        }

        try {
            // India prefix +91
            String formattedPhone = phone.startsWith("+") ? phone : "+91" + phone;
            
            Message message = Message.creator(
                    new PhoneNumber(formattedPhone),
                    new PhoneNumber(twilioPhoneNumber),
                    messageContent
            ).create();

            log.info("SMS sent to {}. Status: {}. SID: {}", phone, message.getStatus(), message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phone, e.getMessage());
        }
    }
}
