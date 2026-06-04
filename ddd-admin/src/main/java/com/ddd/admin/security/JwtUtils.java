package com.ddd.admin.security;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类 — 生成、解析、验证 Token
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;

    @Value("${jwt.expire:7200}")
    private Long expire;           // Access Token 过期时间（秒）

    @Value("${jwt.refresh-expire:604800}")
    private Long refreshExpire;    // Refresh Token 过期时间（秒）

    private final RedisTemplate<String, String> redisTemplate;

    public JwtUtils(@Value("${jwt.secret}") String secret, RedisTemplate<String, String> redisTemplate) {
        // 使用 HMAC-SHA256 密钥
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
    }

    // ======================== 生成 Token ========================

    /**
     * 生成 Access Token
     */
    public String generateToken(Long userId, String username, Set<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire * 1000);

        return Jwts.builder()
                .id(String.valueOf(userId))
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpire * 1000);

        return Jwts.builder()
                .id(String.valueOf(userId))
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // ======================== 解析 Token ========================

    /**
     * 从 Token 中获取 Claims
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getId());
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 获取角色列表
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRoles(String token) {
        return (Set<String>) getClaims(token).get("roles", Set.class);
    }

    /**
     * 获取过期时间
     */
    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    // ======================== 验证 Token ========================

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 1. 检查黑名单
            if (isTokenBlacklisted(token)) {
                log.warn("Token在黑名单中");
                return false;
            }

            // 2. 解析并验证有效期
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String blackKey = "token:blacklist:" + token;
        // 检查 Redis 中是否存在该 Token
        return Boolean.TRUE.equals(redisTemplate.hasKey(blackKey));
    }

    /**
     * 获取 Token 剩余有效时间（秒）
     */
    public Long getRemainingTime(String token) {
        try {
            Date expiration = getExpiration(token);
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0L;
        }
    }

    // ======================== Getter ========================

    public Long getExpire() {
        return expire;
    }

    public Long getRefreshExpire() {
        return refreshExpire;
    }
}
