package com.credixa.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            
            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwt = authToken.substring(7);
                if (tokenProvider.validateToken(jwt)) {
                    String userCode = tokenProvider.getUserCodeFromJWT(jwt);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userCode, null, Collections.emptyList());
                    accessor.setUser(auth);
                    log.info("WebSocket connected for user: {}", userCode);
                } else {
                    log.warn("Invalid JWT token for WebSocket connection");
                }
            }
        }
        return message;
    }
}
