package com.techfork.global.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class AppleClientSecretGenerator {

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.private-key-path}")
    private String privateKeyPath;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    /**
     * Apple client_secret JWT를 생성합니다.
     */
    public String generateClientSecret() {
        try {
            Date expirationDate = Date.from(
                    LocalDateTime.now().plusDays(180)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
            );

            return Jwts.builder()
                    .setHeaderParam("kid", keyId)
                    .setHeaderParam("alg", "ES256")
                    .setIssuer(teamId)
                    .setIssuedAt(new Date())
                    .setExpiration(expirationDate)
                    .setAudience("https://appleid.apple.com")
                    .setSubject(clientId)
                    .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate Apple client secret", e);
            throw new RuntimeException("Apple client secret 생성 실패", e);
        }
    }

    /**
     * private key 파일을 읽어서 PrivateKey 객체로 변환합니다.
     */
    private PrivateKey getPrivateKey() throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource(privateKeyPath);
            String privateKeyContent = new String(Files.readAllBytes(resource.getFile().toPath()));

            // PEM 파일에서 헤더/푸터 제거
            privateKeyContent = privateKeyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            return keyFactory.generatePrivate(keySpec);
        } catch (IOException e) {
            log.error("Failed to load private key from path: {}", privateKeyPath, e);
            throw new RuntimeException("Apple private key 파일 로드 실패", e);
        }
    }
}
