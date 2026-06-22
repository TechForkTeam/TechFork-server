package com.techfork.auth.infrastructure.kakao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoSocialIdTest {

    @Nested
    @DisplayName("Kakao service user ID 정규화")
    class NormalizeKakaoServiceUserId {

        @Test
        @DisplayName("REST id와 OIDC sub를 같은 Kakao service user ID 문자열로 정규화한다")
        void restIdAndOidcSubject_ReturnSameServiceUserId() {
            Long restUserId = 12345L;
            String oidcSubject = "12345";

            assertThat(KakaoSocialId.fromRestUserId(restUserId)).isEqualTo("12345");
            assertThat(KakaoSocialId.fromOidcSubject(oidcSubject)).isEqualTo("12345");
        }

        @Test
        @DisplayName("REST id가 없으면 socialId로 사용할 수 없다")
        void nullRestUserId_ThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> KakaoSocialId.fromRestUserId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("REST user id");
        }

        @Test
        @DisplayName("OIDC sub가 없으면 socialId로 사용할 수 없다")
        void blankOidcSubject_ThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> KakaoSocialId.fromOidcSubject(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subject");
        }
    }

}
