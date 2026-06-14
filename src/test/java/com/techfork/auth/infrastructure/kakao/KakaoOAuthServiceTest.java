package com.techfork.auth.infrastructure.kakao;

import com.techfork.auth.infrastructure.kakao.dto.KakaoUserInfoResponse;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private KakaoOAuthService kakaoOAuthService;

    private String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
    private String validAccessToken = "valid.kakao.access.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoOAuthService, "userInfoUrl", userInfoUrl);

        // WebClient 체이닝 모킹
        given(webClientBuilder.build()).willReturn(webClient);
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 성공")
    void getUserInfo_Success() {
        // Given
        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile("https://example.com/profile.jpg");
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount("test@kakao.com", profile);
        KakaoUserInfoResponse expectedResponse = new KakaoUserInfoResponse(12345L, kakaoAccount);

        given(responseSpec.bodyToMono(KakaoUserInfoResponse.class))
                .willReturn(Mono.just(expectedResponse));

        // When
        KakaoUserInfoResponse result = kakaoOAuthService.getUserInfo(validAccessToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(12345L);
        assertThat(result.id().toString()).isEqualTo("12345");
        assertThat(result.kakaoAccount().email()).isEqualTo("test@kakao.com");
        assertThat(result.kakaoAccount().profile().profileImageUrl()).isEqualTo("https://example.com/profile.jpg");

        verify(webClientBuilder).build();
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(userInfoUrl);
        verify(requestHeadersSpec).header("Authorization", "Bearer " + validAccessToken);
        verify(requestHeadersSpec).retrieve();
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 실패 - 잘못된 액세스 토큰 (401)")
    void getUserInfo_Fail_InvalidAccessToken() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(
                401,
                "Unauthorized",
                null,
                null,
                null
        );

        given(responseSpec.bodyToMono(KakaoUserInfoResponse.class))
                .willReturn(Mono.error(exception));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(validAccessToken))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.INVALID_KAKAO_ACCESS_TOKEN);

        verify(webClientBuilder).build();
        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 실패 - 카카오 API 에러 (500)")
    void getUserInfo_Fail_KakaoApiError() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(
                500,
                "Internal Server Error",
                null,
                null,
                null
        );

        given(responseSpec.bodyToMono(KakaoUserInfoResponse.class))
                .willReturn(Mono.error(exception));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(validAccessToken))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.INVALID_KAKAO_ACCESS_TOKEN);

        verify(webClientBuilder).build();
        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 실패 - 예상치 못한 에러")
    void getUserInfo_Fail_UnexpectedError() {
        // Given
        given(responseSpec.bodyToMono(KakaoUserInfoResponse.class))
                .willReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(validAccessToken))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.KAKAO_API_ERROR);

        verify(webClientBuilder).build();
        verify(webClient).get();
    }
}
