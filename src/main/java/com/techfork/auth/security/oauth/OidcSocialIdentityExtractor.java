package com.techfork.auth.security.oauth;

import com.techfork.auth.infrastructure.kakao.KakaoSocialId;
import com.techfork.useraccount.domain.enums.SocialType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class OidcSocialIdentityExtractor {

    public OidcSocialIdentity extract(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.fromRegistrationId(registrationId);
        String socialId = resolveSocialId(socialType, oidcUser);
        String email = oidcUser.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("email not found");
        }
        String profileImage = oidcUser.getAttribute("picture");

        return new OidcSocialIdentity(socialType, socialId, email, profileImage);
    }

    private String resolveSocialId(SocialType socialType, OidcUser oidcUser) {
        String subject = oidcUser.getAttribute("sub");
        if (subject == null) {
            throw new OAuth2AuthenticationException("socialId(sub) not found");
        }
        if (socialType == SocialType.KAKAO) {
            return KakaoSocialId.fromOidcSubject(subject);
        }
        return subject;
    }
}
