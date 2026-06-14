package com.techfork.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DeveloperTokenController 통합 테스트
 * - 개발자 토큰 발급 API 테스트
 * - ADMIN 권한 검증
 * - 실제 JWT 토큰 사용으로 전체 인증 흐름 테스트
 */
class DeveloperTokenControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User adminUser;
    private User normalUser;
    private String adminAccessToken;
    private String normalUserAccessToken;

    @BeforeEach
    void setUp() {
        // 관리자 사용자 생성
        adminUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("adminSocialId")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        adminUser = userRepository.save(adminUser);

        JwtDTO adminTokens = jwtUtil.generateTokens(adminUser.getId(), Role.ADMIN);
        adminAccessToken = adminTokens.accessToken();

        // 일반 사용자 생성
        normalUser = User.createSocialUser(SocialType.KAKAO, "userSocialId", "user@example.com", null);
        normalUser = userRepository.save(normalUser);

        JwtDTO userTokens = jwtUtil.generateTokens(normalUser.getId(), Role.USER);
        normalUserAccessToken = userTokens.accessToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ===== 개발자 토큰 발급 성공 테스트 =====

    @Test
    @DisplayName("개발자 토큰 발급 성공 - ADMIN 권한으로 30일 만료 토큰 발급")
    void generateDeveloperToken_Success_AdminUser() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/admin/developer-token")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.developerToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.developerToken").isString());
    }

    @Test
    @DisplayName("개발자 토큰 발급 성공 - 발급된 토큰이 유효한 액세스 토큰인지 검증 (실제 JWT 검증)")
    void generateDeveloperToken_Success_TokenIsValid() throws Exception {
        // Given - 실제 JWT 토큰으로 개발자 토큰 발급
        JwtDTO adminTokens = jwtUtil.generateTokens(adminUser.getId(), Role.ADMIN);

        // When
        String responseBody = mockMvc.perform(post("/api/v1/admin/developer-token")
                        .header("Authorization", "Bearer " + adminTokens.accessToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - 발급된 토큰이 유효한 액세스 토큰인지 검증
        ObjectMapper objectMapper = new ObjectMapper();
        String developerToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("developerToken")
                .asText();

        // 토큰이 유효하고 타입이 access인지 검증
        assertThat(jwtUtil.isValidToken(developerToken)).isTrue();
        assertThat(jwtUtil.getTokenType(developerToken)).isEqualTo("access");
    }

    // ===== 개발자 토큰 발급 실패 테스트 =====

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 일반 사용자 권한으로 접근 시 403 Forbidden")
    void generateDeveloperToken_Fail_NormalUserForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/admin/developer-token")
                        .header("Authorization", "Bearer " + normalUserAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON403"));
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 인증 없이 접근 시 401 Unauthorized")
    void generateDeveloperToken_Fail_NoAuthenticationUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/admin/developer-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"));
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 유효하지 않은 토큰으로 접근 (실제 JWT 검증)")
    void generateDeveloperToken_Fail_InvalidToken() throws Exception {
        // When & Then - 실제 JWT 필터를 통과해야 하므로 실제 토큰 사용
        mockMvc.perform(post("/api/v1/admin/developer-token")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_MALFORMED"));
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 존재하지 않는 사용자")
    void generateDeveloperToken_Fail_UserNotFound() throws Exception {
        // Given - 존재하지 않는 사용자 ID로 토큰 생성
        JwtDTO fakeAdminTokens = jwtUtil.generateTokens(999999L, Role.ADMIN);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/developer-token")
                        .header("Authorization", "Bearer " + fakeAdminTokens.accessToken()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.USER_NOT_FOUND.getReason().code()));
    }
}
