package com.leafy.apigateway.filter;

import com.leafy.common.utils.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    JwtUtil jwtUtil;

    @Qualifier("reactiveRedisTemplate")
    ReactiveRedisTemplate<String, String> redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                    @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        
        // Validate token
        try {
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return onError(exchange, "JWT validation failed", HttpStatus.UNAUTHORIZED);
        }

        // Check if token is blacklisted (for logout functionality)
        String tokenKey = "blacklist:token:" + token;
        return redisTemplate.hasKey(tokenKey)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return onError(exchange, "Token has been revoked", HttpStatus.UNAUTHORIZED);
                    }

                    // Extract user information and add to request headers
                    String userId = jwtUtil.extractUserId(token);
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    String sessionId = jwtUtil.extractSessionId(token);
                    String jti = jwtUtil.extractJti(token);
                    
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-Auth-User-Id", userId)
                            .header("X-Auth-Email", email != null ? email : "")
                            .header("X-Auth-Role", role != null ? role : "")
                            .header("X-Auth-Session-Id", sessionId != null ? sessionId : "")
                            .header("X-Auth-Jti", jti != null ? jti : "")
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Error checking token blacklist: {}", e.getMessage());
                    return onError(exchange, "Authentication service error", HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        log.error("Authentication error: {}", message);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
