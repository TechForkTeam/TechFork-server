package com.techfork.auth.application;

import com.techfork.auth.application.dto.KakaoLoginResponse;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.dto.KakaoUserInfoResponse;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.security.auth.service.RefreshTokenService;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtProperties;
import com.techfork.global.security.jwt.JwtUtil;
import com.techfork.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoLoginService {

    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuthConverter authConverter;

    @Value("${server.domain}")
    private String domain;

    public KakaoLoginResponse login(String kakaoAccessToken, HttpServletResponse response) {
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

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
        CookieUtil.addRefreshTokenCookie(response, domain, tokens.refreshToken(), expiration);

        log.info("Direct Kakao login successful - userId: {}, isRegistered: {}", user.getId(), user.isActive());

        return authConverter.toKakaoLoginResponse(tokens.accessToken(), user);
    }
}
