package com.techfork.auth.security.oauth;

import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Spring Security 인증 주체 (Principal)
 * - UserDetails: JWT 인증에서 사용
 * - OidcUser: OAuth2 로그인에서 사용
 * 두 인터페이스를 모두 구현하여 다양한 인증 방식 지원
 *
 * 최소 필드만 포함:
 * - id: 사용자 고유 식별자
 * - role: 권한 (ADMIN, USER)
 * - status: 계정 상태 (PENDING, ACTIVE)
 * - email: 사용자 이메일
 * - attributes: OAuth2 로그인용 속성 (일반 JWT에서는 null)
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails, OidcUser {

    private final Long id;
    private final Role role;
    private final UserStatus status;
    private final String email;
    private final Map<String, Object> attributes;

    // ===== UserDetails 구현 =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.getKey()));
    }

    @Override
    public String getPassword() {
        // 소셜 로그인 사용자이므로 비밀번호 없음
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 현재는 PENDING, ACTIVE만 존재하므로 항상 잠기지 않음
        // 추후 SUSPENDED, DELETED 추가 시 status != SUSPENDED && status != DELETED로 변경
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING;
    }

    // ===== OidcUser 구현 =====

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return null;
    }

    public static UserPrincipal from(UserAuthProfile userAuthProfile) {
        return UserPrincipal.builder()
                .id(userAuthProfile.id())
                .role(userAuthProfile.role())
                .status(userAuthProfile.status())
                .email(userAuthProfile.email())
                .build();
    }
}
