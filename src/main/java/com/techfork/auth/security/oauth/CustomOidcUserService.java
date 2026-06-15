package com.techfork.auth.security.oauth;

import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
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
    private final OidcSocialIdentityExtractor socialIdentityExtractor;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        OidcSocialIdentity socialIdentity = socialIdentityExtractor.extract(userRequest, oidcUser);

        UserAuthProfile userAuthProfile = userAuthAccountService.getOrCreateSocialAuthProfile(
                socialIdentity.socialType(),
                socialIdentity.socialId(),
                socialIdentity.email(),
                socialIdentity.profileImage()
        );

        log.info("CustomOAuth2UserService - loaded user: id={}, email={}, socialType={}",
                userAuthProfile.id(), socialIdentity.email(), socialIdentity.socialType());

        return UserPrincipal.from(userAuthProfile);
    }
}
