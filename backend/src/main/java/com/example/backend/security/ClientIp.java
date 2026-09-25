package com.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ClientIp {
    private ClientIp() {
    }

    /**
     * nginx overwrites X-Real-IP with the peer address, and the backend only listens on 127.0.0.1
     * (docker-compose.yml), so the header cannot be supplied by a client. Null when there is no request.
     */
    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String real = request.getHeader("X-Real-IP");
            return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
        }
        return null;
    }
}
