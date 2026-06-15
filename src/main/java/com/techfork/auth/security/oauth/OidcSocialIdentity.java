package com.techfork.auth.security.oauth;

import com.techfork.useraccount.domain.enums.SocialType;

public record OidcSocialIdentity(
        SocialType socialType,
        String socialId,
        String email,
        String profileImage
) {
}
