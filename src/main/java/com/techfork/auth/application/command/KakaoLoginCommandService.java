package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.response.KakaoUserInfoResponse;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoLoginCommandService {

    private final KakaoOAuthService kakaoOAuthService;
    private final UserAuthAccountService userAuthAccountService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public KakaoLoginResult login(KakaoLoginCommand command) {
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(command.accessToken());

        String socialId = kakaoUserInfo.id().toString();
        String email = kakaoUserInfo.kakaoAccount().email();
        String profileImageUrl = kakaoUserInfo.kakaoAccount().profile().profileImageUrl();

        UserAuthProfile userAuthProfile = userAuthAccountService.getOrCreateSocialAuthProfile(
                SocialType.KAKAO,
                socialId,
                email,
                profileImageUrl
        );

        JwtDTO tokens = jwtUtil.generateTokens(userAuthProfile.id(), userAuthProfile.role());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        refreshTokenService.saveRefreshToken(userAuthProfile.id(), tokens.refreshToken(), expiration);

        log.info("Direct Kakao login successful - userId: {}, isRegistered: {}",
                userAuthProfile.id(), userAuthProfile.active());

        return KakaoLoginResult.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .refreshTokenExpiration(expiration)
                .userId(userAuthProfile.id())
                .isRegistered(userAuthProfile.active())
                .build();
    }
}
