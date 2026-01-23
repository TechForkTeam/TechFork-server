package com.techfork.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.user.dto.OnboardingRequest;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OnboardingController 통합 테스트
 */
class OnboardingControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private LlmClient llmClient;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        testUser = userRepository.save(testUser);

        // PENDING 상태 사용자용 JWT 토큰 생성
        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();

        // LLM 클라이언트 모킹 - 테스트용 더미 응답 반환
        when(llmClient.call(anyString(), anyString()))
                .thenReturn("테스트용 사용자 프로필입니다. 백엔드 개발에 관심이 많습니다.");

        // Embedding 클라이언트 모킹 - 테스트용 더미 벡터 반환
        when(embeddingClient.embed(anyString()))
                .thenReturn(Collections.nCopies(3072, 0.1f));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("관심사 목록 조회 - 성공")
    void getInterests_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/onboarding/interests"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.categories").isNotEmpty());
    }

    @Test
    @DisplayName("온보딩 완료 - 정상 케이스")
    void completeOnboarding_Success() throws Exception {
        // Given
        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING"))
                        .build(),
                UserInterestDto.builder()
                        .category("DATABASE")
                        .keywords(List.of("MYSQL", "REDIS"))
                        .build()
        );

        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                "백엔드 개발자입니다",
                interests
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 데이터베이스 검증
        User savedUser = userRepository.findByIdWithInterestCategories(testUser.getId()).orElseThrow();
        assertThat(savedUser.getNickName()).isEqualTo("테크포크유저");
        assertThat(savedUser.getEmail()).isEqualTo("user@techfork.com");
        assertThat(savedUser.getDescription()).isEqualTo("백엔드 개발자입니다");
        assertThat(savedUser.getInterestCategories()).hasSize(2);
    }

    @Test
    @DisplayName("온보딩 완료 - description null 허용")
    void completeOnboarding_NullDescription_Success() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("FRONTEND")
                                .keywords(List.of("REACT"))
                                .build()
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        User savedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(savedUser.getDescription()).isNull();
    }

    @Test
    @DisplayName("온보딩 완료 - 닉네임 필수 검증")
    void completeOnboarding_BlankNickname_BadRequest() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
                "",
                "user@techfork.com",
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 완료 - 닉네임 길이 검증 (2-20자)")
    void completeOnboarding_NicknameTooShort_BadRequest() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
                "a", // 1자 (최소 2자 필요)
                "user@techfork.com",
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 완료 - 이메일 형식 검증")
    void completeOnboarding_InvalidEmail_BadRequest() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "invalid-email", // 잘못된 이메일 형식
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 완료 - 관심사 필수 검증")
    void completeOnboarding_EmptyInterests_BadRequest() throws Exception {
        // Given
        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of() // 빈 관심사 목록
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 완료 - description 길이 검증 (100자 이하)")
    void completeOnboarding_DescriptionTooLong_BadRequest() throws Exception {
        // Given
        String longDescription = "a".repeat(101); // 101자

        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                longDescription,
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 완료 - 여러 카테고리와 키워드 조합")
    void completeOnboarding_MultipleCategories_Success() throws Exception {
        // Given
        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING", "PYTHON"))
                        .build(),
                UserInterestDto.builder()
                        .category("DEVOPS")
                        .keywords(List.of("DOCKER", "KUBERNETES"))
                        .build(),
                UserInterestDto.builder()
                        .category("DATABASE")
                        .keywords(List.of("MYSQL", "POSTGRESQL", "REDIS"))
                        .build()
        );

        OnboardingRequest request = new OnboardingRequest(
                "풀스택개발자",
                "fullstack@techfork.com",
                "백엔드와 인프라를 다룹니다",
                interests
        );

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        User savedUser = userRepository.findByIdWithInterestCategories(testUser.getId()).orElseThrow();
        assertThat(savedUser.getInterestCategories()).hasSize(3);
    }
}
