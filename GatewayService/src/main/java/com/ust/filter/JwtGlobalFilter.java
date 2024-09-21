package com.ust.filter;

import com.ust.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtGlobalFilter.class);

    private final JwtConfig jwtConfig;

    public JwtGlobalFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        logger.info("Incoming request path: {}", path);

        if (isSecuredPath(request)) {
            logger.info("Path {} is secured, checking for JWT", path);
            if (!request.getHeaders().containsKey(jwtConfig.getHeaderString())) {
                logger.warn("No Authorization header found");
                return this.onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(jwtConfig.getHeaderString());
            logger.info("Authorization header: {}", authHeader);
            if (authHeader == null || !authHeader.startsWith(jwtConfig.getTokenPrefix())) {
                logger.warn("Invalid Authorization header format");
                return this.onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.replace(jwtConfig.getTokenPrefix(), "");
            logger.info("Extracted token: {}", token);

            if (!isValidToken(token)) {
                logger.warn("Invalid token");
                return this.onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
            logger.info("Token is valid");
        } else {
            logger.info("Path {} is not secured, skipping JWT check", path);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isSecuredPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        // Exclude registration endpoint from JWT check
        return path.startsWith("/api/") && !path.equals("/api/employees/register");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        logger.error("Authentication error: {}", err);
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public boolean isValidToken(String token) {
        try {
            logger.info("Validating token");
            Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            logger.info("Token validation successful. Claims: {}", claims);
            return true;
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }
}