package com.example.authservice.util;



import com.example.authservice.util.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final Key key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     */
    public String generateToken(String username) {
        return generateToken(null, username, null);
    }

    /**
     * 生成带用户信息和会话信息的 JWT token
     */
    public String generateToken(Long userId, String username, String sessionId) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtProperties.getExpire()));

        if (userId != null) {
            builder.claim("uid", userId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 解析 token 获取用户名
     */
    public String parseUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 解析 token 获取用户 ID
     */
    public Long parseUserId(String token) {
        Object value = parseClaims(token).get("uid");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(Objects.toString(value));
    }

    /**
     * 解析 token 获取会话 ID
     */
    public String parseSessionId(String token) {
        Object value = parseClaims(token).get("sid");
        return value == null ? null : Objects.toString(value);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 判断 token 是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
