package com.techfork.global.security.jwt;

import com.techfork.domain.auth.exception.AuthErrorCode;
import com.techfork.domain.user.enums.Role;
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

    public boolean validateToken(String token) {
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
        return true;
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
