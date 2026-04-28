package com.techfork.domain.useraccount.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.useraccount.dto.UpdateAccountProfileRequest;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.Role;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.enums.UserStatus;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 통합 테스트
 */
class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // ACTIVE 상태의 테스트 사용자 생성
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser.updateUser("테스트유저", "test@example.com", "백엔드 개발자입니다.");
        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ===== 프로필 조회 테스트 =====

    @Test
    @DisplayName("내 계정 프로필 조회 성공")
    void getMyProfile_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.profileImage").value("profile.jpg"))
                .andExpect(jsonPath("$.data.nickName").value("테스트유저"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.description").value("백엔드 개발자입니다."));
    }

    // ===== 프로필 수정 테스트 =====

    @Test
    @DisplayName("내 계정 프로필 수정 성공 - 닉네임만 수정")
    void updateMyProfile_Success_OnlyNickName() throws Exception {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest("새로운닉네임", null);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB에서 변경사항 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getNickName()).isEqualTo("새로운닉네임");
        assertThat(updatedUser.getDescription()).isEqualTo("백엔드 개발자입니다."); // 변경되지 않음
    }

    @Test
    @DisplayName("내 계정 프로필 수정 성공 - 자기소개만 수정")
    void updateMyProfile_Success_OnlyDescription() throws Exception {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest(null, "프론트엔드 개발자로 전향했습니다.");

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB에서 변경사항 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getNickName()).isEqualTo("테스트유저"); // 변경되지 않음
        assertThat(updatedUser.getDescription()).isEqualTo("프론트엔드 개발자로 전향했습니다.");
    }

    @Test
    @DisplayName("내 계정 프로필 수정 성공 - 닉네임과 자기소개 모두 수정")
    void updateMyProfile_Success_BothFields() throws Exception {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest("풀스택개발자", "백엔드와 프론트엔드 모두 합니다.");

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB에서 변경사항 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getNickName()).isEqualTo("풀스택개발자");
        assertThat(updatedUser.getDescription()).isEqualTo("백엔드와 프론트엔드 모두 합니다.");
    }

    @Test
    @DisplayName("내 계정 프로필 수정 성공 - 빈 요청 (아무것도 수정하지 않음)")
    void updateMyProfile_Success_EmptyRequest() throws Exception {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest(null, null);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB에서 변경사항 확인 (아무것도 변경되지 않음)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getNickName()).isEqualTo("테스트유저");
        assertThat(updatedUser.getDescription()).isEqualTo("백엔드 개발자입니다.");
    }

    @Test
    @DisplayName("계정 프로필 수정 후 조회 - 변경사항 반영 확인")
    void updateAndGetProfile_Success() throws Exception {
        // Given
        UpdateAccountProfileRequest updateRequest = new UpdateAccountProfileRequest("변경된닉네임", "변경된 자기소개");

        // When - 프로필 수정
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Then - 프로필 조회로 변경사항 확인
        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.nickName").value("변경된닉네임"))
                .andExpect(jsonPath("$.data.description").value("변경된 자기소개"))
                .andExpect(jsonPath("$.data.email").value("test@example.com")) // 변경되지 않음
                .andExpect(jsonPath("$.data.profileImage").value("profile.jpg")); // 변경되지 않음
    }

    // ===== 회원 탈퇴 테스트 =====

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdrawUser_Success() throws Exception {
        // When
        mockMvc.perform(patch("/api/v1/users/me/withdrawal")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // Then - DB에서 탈퇴 상태 및 개인정보 익명화 확인
        User withdrawnUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(withdrawnUser.isWithdrawn()).isTrue();

        // 개인정보 익명화 확인
        assertThat(withdrawnUser.getNickName()).isNull();
        assertThat(withdrawnUser.getEmail()).isNull();
        assertThat(withdrawnUser.getProfileImage()).isNull();
        assertThat(withdrawnUser.getDescription()).isNull();

        // socialId는 유지 (재가입 시 사용)
        assertThat(withdrawnUser.getSocialId()).isEqualTo("testSocialId");
        assertThat(withdrawnUser.getSocialType()).isEqualTo(SocialType.KAKAO);
    }
}
