package com.credixa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@Slf4j
public class WebSocketController {

    @MessageMapping("/account/subscribe")
    @SendToUser("/queue/balance")
    public String subscribe(Principal principal) {
        log.info("Client subscribed to balance updates: {}", principal.getName());
        return "Subscribed to live balance updates";
    }
}
