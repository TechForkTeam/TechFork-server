package com.techfork.global.security.oauth;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.fromRegistrationId(registrationId);

        String socialId = oAuth2User.getAttribute("sub");
        if (socialId == null) {
            throw new OAuth2AuthenticationException("socialId(sub) not found");
        }
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("email not found");
        }

        User user = getOrCreateUser(socialType, socialId, email);

        log.info("CustomOAuth2UserService - loaded user: id={}, email={}, socialType={}",
                user.getId(), email, socialType);

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickName())
                .socialType(user.getSocialType())
                .socialId(user.getSocialId())
                .role(user.getRole())
                .status(user.getStatus())
                .attributes(oAuth2User.getAttributes())
                .build();
    }

    private User getOrCreateUser(SocialType socialType, String socialId, String email) {
        return userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .orElseGet(() -> {
                    User newUser = User.createSocialUser(socialType, socialId, email);
                    User savedUser = userRepository.save(newUser);
                    log.info("New user created - id: {}, socialType: {}, socialId: {}, email: {}",
                            savedUser.getId(), socialType, socialId, email);
                    return savedUser;
                });
    }
}
