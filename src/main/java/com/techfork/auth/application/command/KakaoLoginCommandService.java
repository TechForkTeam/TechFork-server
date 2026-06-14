package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.dto.KakaoUserInfoResponse;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
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
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public KakaoLoginResult login(KakaoLoginCommand command) {
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(command.accessToken());

        String socialId = kakaoUserInfo.id().toString();
        String email = kakaoUserInfo.kakaoAccount().email();
        String profileImageUrl = kakaoUserInfo.kakaoAccount().profile().profileImageUrl();

        User user = userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)
                .orElseGet(() -> {
                    User newUser = User.createSocialUser(SocialType.KAKAO, socialId, email, profileImageUrl);
                    return userRepository.save(newUser);
                });

        JwtDTO tokens = jwtUtil.generateTokens(user.getId(), user.getRole());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        refreshTokenService.saveRefreshToken(user.getId(), tokens.refreshToken(), expiration);

        log.info("Direct Kakao login successful - userId: {}, isRegistered: {}", user.getId(), user.isActive());

        return KakaoLoginResult.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .refreshTokenExpiration(expiration)
                .userId(user.getId())
                .isRegistered(user.isActive())
                .build();
    }
}
