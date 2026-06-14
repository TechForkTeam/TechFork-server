package com.techfork.auth.infrastructure.kakao;

import com.techfork.auth.infrastructure.kakao.dto.KakaoUserInfoResponse;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUrl;

    private final WebClient.Builder webClientBuilder;

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(userInfoUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfoResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to get Kakao user info. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(AuthErrorCode.INVALID_KAKAO_ACCESS_TOKEN);
        } catch (Exception e) {
            log.error("Unexpected error while getting Kakao user info", e);
            throw new GeneralException(AuthErrorCode.KAKAO_API_ERROR);
        }
    }
}
