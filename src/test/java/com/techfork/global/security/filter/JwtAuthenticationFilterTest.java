package com.techfork.global.security.filter;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.enums.UserStatus;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.security.jwt.JwtUtil;
import com.techfork.global.security.oauth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.techfork.global.security.jwt.JwtConstants.TOKEN_TYPE_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private Long userId;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        userId = 1L;
        validAccessToken = "valid.access.token";

        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        ReflectionTestUtils.setField(testUser, "id", userId);
    }

    // ===== 인증 성공 테스트 =====

    @Test
    @DisplayName("JWT 인증 성공 - 유효한 액세스 토큰으로 SecurityContext 설정")
    void doFilterInternal_Success_WithValidAccessToken() throws Exception {
        // Given
        given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
        willDoNothing().given(jwtUtil).validateToken(validAccessToken);
        given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getRole()).isEqualTo(Role.USER);
        assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(principal.getUsername()).isEqualTo(String.valueOf(userId));

        verify(jwtUtil).validateToken(validAccessToken);
        verify(jwtUtil).validateTokenType(validAccessToken, TOKEN_TYPE_ACCESS);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    // ===== 인증 실패 테스트 =====

    @Test
    @DisplayName("JWT 인증 실패 - Authorization 헤더 없음")
    void doFilterInternal_Fail_NoAuthorizationHeader() throws Exception {
        // Given
        given(request.getHeader("Authorization")).willReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 인증 실패 - Bearer 접두사 없음")
    void doFilterInternal_Fail_NoBearerPrefix() throws Exception {
        // Given
        given(request.getHeader("Authorization")).willReturn(validAccessToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 인증 실패 - 유효하지 않은 토큰")
    void doFilterInternal_Fail_InvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
        willThrow(new RuntimeException("Invalid token")).given(jwtUtil).validateToken(invalidToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(invalidToken);
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 인증 실패 - 리프레시 토큰 사용 시도")
    void doFilterInternal_Fail_RefreshTokenUsed() throws Exception {
        // Given
        String refreshToken = "refresh.token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + refreshToken);
        willDoNothing().given(jwtUtil).validateToken(refreshToken);
        doThrow(new RuntimeException("Token type mismatch"))
                .when(jwtUtil).validateTokenType(refreshToken, TOKEN_TYPE_ACCESS);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).validateTokenType(refreshToken, TOKEN_TYPE_ACCESS);
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 인증 실패 - 사용자를 찾을 수 없음 (탈퇴 또는 삭제된 사용자)")
    void doFilterInternal_Fail_UserNotFound() throws Exception {
        // Given
        String deletedUserToken = "deleted.user.token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + deletedUserToken);
        willDoNothing().given(jwtUtil).validateToken(deletedUserToken);
        willDoNothing().given(jwtUtil).validateTokenType(deletedUserToken, TOKEN_TYPE_ACCESS);
        given(jwtUtil.getUserIdFromToken(deletedUserToken)).willReturn(999L);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(deletedUserToken);
        verify(jwtUtil).validateTokenType(deletedUserToken, TOKEN_TYPE_ACCESS);
        verify(userRepository).findById(999L);
        verify(filterChain).doFilter(request, response);
    }
}
