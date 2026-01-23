package com.techfork.global.security.oauth;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.fromRegistrationId(registrationId);

        String socialId = oidcUser.getAttribute("sub");
        if (socialId == null) {
            throw new OAuth2AuthenticationException("socialId(sub) not found");
        }
        String email = oidcUser.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("email not found");
        }
        String profileImage = oidcUser.getAttribute("picture");

        User user = getOrCreateUser(socialType, socialId, email, profileImage);

        log.info("CustomOAuth2UserService - loaded user: id={}, email={}, socialType={}",
                user.getId(), email, socialType);

        return UserPrincipal.buildUserPrincipal(user);
    }

    private User getOrCreateUser(SocialType socialType, String socialId, String email, String profileImage) {
        return userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .orElseGet(() -> {
                    User newUser = User.createSocialUser(socialType, socialId, email, profileImage);
                    User savedUser = userRepository.save(newUser);
                    log.info("New user created - id: {}, socialType: {}, socialId: {}, email: {}, profileImage: {}",
                            savedUser.getId(), socialType, socialId, email, profileImage);
                    return savedUser;
                });
    }
}
