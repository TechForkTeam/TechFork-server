package com.techfork.global.security.jwt;

import com.techfork.domain.auth.exception.AuthErrorCode;
import com.techfork.domain.useraccount.enums.Role;
import com.techfork.global.exception.GeneralException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.techfork.global.security.jwt.JwtConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtDTO generateTokens(Long userId, Role role) {
        return JwtDTO.of(
                generateToken(userId, role, jwtProperties.getAccessTokenExpiration(), TOKEN_TYPE_ACCESS),
                generateToken(userId, role, jwtProperties.getRefreshTokenExpiration(), TOKEN_TYPE_REFRESH)
        );
    }

    public String generateLongLivedAccessToken(Long userId, Role role) {
        // 30일 = 30일 * 24시간 * 60분 * 60초 * 1000밀리초
        long longLivedTokenExpiration = 30L * 24 * 60 * 60 * 1000;
        return generateToken(userId, role, longLivedTokenExpiration, TOKEN_TYPE_ACCESS);
    }

    private String generateToken(Long userId, Role role, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        if (role != null) {
            builder.claim(CLAIM_USER_ROLE, role.name());
        }

        return builder.compact();
    }

    /**
     * JWT 토큰 유효성 검증 (예외 발생)
     * - 유효하지 않은 경우 구체적인 예외를 던짐 (ExpiredJwtException, MalformedJwtException 등)
     * - JWT 필터에서 사용하여 예외 타입별로 다른 에러 응답 반환
     */
    public void validateToken(String token) {
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * JWT 토큰 유효성 검증 (boolean 반환)
     * - 유효하면 true, 유효하지 않으면 false 반환
     * - 간단한 유효성 체크가 필요한 경우 사용
     */
    public boolean isValidToken(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void validateTokenType(String token, String expectedType) {
        String tokenType = getTokenType(token);
        if (!expectedType.equals(tokenType)) {
            throw new GeneralException(AuthErrorCode.TOKEN_TYPE_MISMATCH);
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
