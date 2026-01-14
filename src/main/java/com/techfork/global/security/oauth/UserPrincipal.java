package com.techfork.global.security.oauth;

import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@Builder
public class UserPrincipal implements OAuth2User {

    private final Long id;
    private final String email;
    private final String nickname;
    private final SocialType socialType;
    private final String socialId;
    private final Role role;
    private final UserStatus status;
    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.getKey()));
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}