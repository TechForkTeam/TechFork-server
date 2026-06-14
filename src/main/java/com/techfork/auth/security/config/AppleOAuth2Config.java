package com.techfork.auth.security.config;

import com.techfork.auth.security.util.AppleClientSecretGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppleOAuth2Config {

    private final AppleClientSecretGenerator appleClientSecretGenerator;

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();

        client.setParametersConverter(grantRequest -> {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

            // 기본 필수 파라미터 세팅
            parameters.add(OAuth2ParameterNames.GRANT_TYPE, grantRequest.getGrantType().getValue());
            parameters.add(OAuth2ParameterNames.CODE, grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());

            // Authorization Request에서 실제 사용된 redirect_uri 가져오기 (템플릿이 아닌 실제 값)
            String redirectUri = grantRequest.getAuthorizationExchange()
                    .getAuthorizationRequest()
                    .getRedirectUri();
            parameters.add(OAuth2ParameterNames.REDIRECT_URI, redirectUri);
            parameters.add(OAuth2ParameterNames.CLIENT_ID, grantRequest.getClientRegistration().getClientId());

            String registrationId = grantRequest.getClientRegistration().getRegistrationId();
            // Apple일 때만 client-secret 동적 생성
            if ("apple".equals(registrationId)) {
                String clientSecret = appleClientSecretGenerator.generateClientSecret();
                parameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);
                log.debug("Apple client-secret generated dynamically");
            } else {
                // 다른 소셜 로그인은 yml 설정 사용
                parameters.add(OAuth2ParameterNames.CLIENT_SECRET, grantRequest.getClientRegistration().getClientSecret());
            }

            return parameters;
        });

        return client;
    }
}
