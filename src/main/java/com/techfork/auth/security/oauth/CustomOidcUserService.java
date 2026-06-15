package com.techfork.auth.security.oauth;

import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.SocialType;
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
public class CustomOidcUserService extends OidcUserService {

    private final UserAuthAccountService userAuthAccountService;

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

        UserAuthProfile userAuthProfile = userAuthAccountService.getOrCreateSocialAuthProfile(
                socialType,
                socialId,
                email,
                profileImage
        );

        log.info("CustomOAuth2UserService - loaded user: id={}, email={}, socialType={}",
                userAuthProfile.id(), email, socialType);

        return UserPrincipal.from(userAuthProfile);
    }
}
