package com.credixa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();

        log.info("[REQUEST] {} {} from {}", method, uri, ip);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("[RESPONSE] {} in {}ms | Status: {}", uri, duration, status);
        }
    }
}
