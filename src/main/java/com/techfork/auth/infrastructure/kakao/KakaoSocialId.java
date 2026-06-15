package com.techfork.auth.infrastructure.kakao;

import org.springframework.util.StringUtils;

/**
 * Kakao 계정 식별자를 TechFork 소셜 로그인 식별자로 정규화한다.
 *
 * <p>Kakao 문서상 REST 사용자 정보 조회 API의 {@code id}와 OIDC ID token의
 * {@code sub}는 같은 Kakao 앱 안에서 사용자를 식별하는 service user ID 계약이다.</p>
 *
 * @see <a href="https://developers.kakao.com/docs/en/kakaologin/rest-api#req-user-info-response-body">Kakao REST user info</a>
 * @see <a href="https://developers.kakao.com/docs/en/kakaologin/utilize#oidc-id-token-payload">Kakao OIDC ID token payload</a>
 */
public final class KakaoSocialId {

    private KakaoSocialId() {
    }

    public static String fromRestUserId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Kakao REST user id must not be null");
        }
        return id.toString();
    }

    public static String fromOidcSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Kakao OIDC subject(sub) must not be blank");
        }
        return subject;
    }
}
