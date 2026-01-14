package com.techfork.global.security.jwt;

import com.techfork.domain.user.enums.Role;
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
public class JwtTokenProvider {

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

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
