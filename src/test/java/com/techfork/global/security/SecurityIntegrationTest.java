package com.techfork.global.security;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 인증/인가 통합 테스트
 * - JWT 인증 실패 시 401 응답 검증
 * - 권한 부족 시 403 응답 검증
 */
class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private LlmClient llmClient;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    private User testUser;
    private String validAccessToken;
    private String adminAccessToken;

    @BeforeEach
    void setUp() {
        // 일반 사용자
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com");
        testUser = userRepository.save(testUser);

        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        validAccessToken = tokens.accessToken();

        // 관리자 사용자
        User adminUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("adminSocialId")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        adminUser = userRepository.save(adminUser);

        JwtDTO adminTokens = jwtUtil.generateTokens(adminUser.getId(), Role.ADMIN);
        adminAccessToken = adminTokens.accessToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ========== 401 Unauthorized 시나리오 ==========

    @Test
    @DisplayName("401 - 토큰 없이 인증이 필요한 엔드포인트 접근")
    void unauthorized_NoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"));
    }

    @Test
    @DisplayName("401 - 만료된 토큰으로 접근")
    void unauthorized_ExpiredToken() throws Exception {
        // Given - 이미 만료된 토큰 생성
        String expiredToken = createExpiredToken(testUser.getId());

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_EXPIRED"));
    }

    @Test
    @DisplayName("401 - 잘못된 형식의 토큰으로 접근")
    void unauthorized_MalformedToken() throws Exception {
        // Given - 잘못된 형식의 토큰
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + malformedToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_MALFORMED"));
    }

    @Test
    @DisplayName("401 - 잘못된 서명의 토큰으로 접근")
    void unauthorized_InvalidSignature() throws Exception {
        // Given - 다른 시크릿 키로 서명된 토큰
        String invalidSignatureToken = createTokenWithWrongSecret(testUser.getId());

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + invalidSignatureToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_SIGNATURE"));
    }

    @Test
    @DisplayName("401 - Refresh 토큰을 Access 토큰 자리에 사용")
    void unauthorized_RefreshTokenUsedAsAccessToken() throws Exception {
        // Given - Refresh 토큰 생성
        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        String refreshToken = tokens.refreshToken();

        // When & Then - Refresh 토큰으로 인증 API 접근 시도
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + refreshToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH400_TYPE_MISMATCH"));
    }

    // ========== 공개 엔드포인트 접근 테스트 ==========

    @Test
    @DisplayName("200 - 인증 없이 공개 엔드포인트 접근 (온보딩 관심사)")
    void publicEndpoint_OnboardingInterests_NoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/interests"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("200 - 인증 없이 공개 엔드포인트 접근 (일반 검색)")
    void publicEndpoint_GeneralSearch_NoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/search/general")
                        .param("query", "test"))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 401이 아니어야 함 (공개 엔드포인트이므로)
                    if (status == 401) {
                        throw new AssertionError("Public endpoint should not return 401 Unauthorized");
                    }
                });
    }

    // ========== 정상 인증 시나리오 ==========

    @Test
    @DisplayName("200 - 유효한 토큰으로 인증 필요 엔드포인트 정상 접근")
    void success_ValidTokenAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 200 또는 다른 성공 응답 (401/403이 아니어야 함)
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Valid token should not be rejected with 401/403");
                    }
                });
    }

    // ========== 403 Forbidden 시나리오 ==========

    @Test
    @DisplayName("403 - 일반 사용자가 관리자 전용 엔드포인트 접근")
    void forbidden_UserAccessAdminEndpoint() throws Exception {
        // When & Then - 일반 사용자 토큰으로 관리자 엔드포인트 접근
        mockMvc.perform(get("/api/v1/batch/run")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON403"));
    }

    @Test
    @DisplayName("200 - 관리자가 관리자 전용 엔드포인트 접근 (정상)")
    void success_AdminAccessAdminEndpoint() throws Exception {
        // When & Then - 관리자 토큰으로 접근 시 정상 처리
        // 실제 배치 실행은 안 되고 404나 다른 에러가 날 수 있지만, 403이 아니어야 함
        mockMvc.perform(get("/api/v1/batch/run")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Expected status code not to be 403 but was 403");
                    }
                });
    }

    // ========== 404 Forbidden 시나리오 ==========

    @Test
    @DisplayName("404 - 존재하지 않는 사용자의 토큰으로 접근")
    void unauthorized_UserNotFound() throws Exception {
        // Given - 존재하지 않는 사용자 ID로 토큰 생성
        JwtDTO tokens = jwtUtil.generateTokens(999999L, Role.USER);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH404_USER"));
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * 이미 만료된 토큰 생성
     */
    private String createExpiredToken(Long userId) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000); // 1초 전 만료

        // application-integrationtest.yml의 jwt.secret 사용
        SecretKey signingKey = Keys.hmacShaKeyFor(
                "test-jwt-secret-key-for-integration-test-must-be-at-least-256-bits".getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .claim("userId", userId)
                .claim("type", "access")
                .claim("role", Role.USER.name())
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(expiredDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 잘못된 시크릿 키로 서명된 토큰 생성
     */
    private String createTokenWithWrongSecret(Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + 900000); // 15분

        // 다른 시크릿 키 사용
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-for-testing-invalid-signature-error-case".getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .claim("userId", userId)
                .claim("type", "access")
                .claim("role", Role.USER.name())
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(wrongKey)
                .compact();
    }
}