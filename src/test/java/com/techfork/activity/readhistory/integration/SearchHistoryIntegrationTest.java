package com.techfork.activity.readhistory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import com.techfork.activity.readhistory.presentation.SearchHistoryRequest;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.MySqlRedisIntegrationTestBase;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchHistoryIntegrationTest extends MySqlRedisIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = UserFixture.activeUser("testSocialId", "test@example.com");
        testUser = userRepository.save(testUser);

        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();
    }

    @AfterEach
    void tearDown() {
        searchHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("검색 히스토리 저장")
    class SaveSearchHistory {

        @Test
        @DisplayName("검색 히스토리 저장 성공")
        void queryFieldProvided_ReturnsOk() throws Exception {
            SearchHistoryRequest request = new SearchHistoryRequest("Spring Boot", LocalDateTime.now());

            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            List<SearchHistory> searchHistories = searchHistoryRepository.findAll();
            assertThat(searchHistories).hasSize(1);
            assertThat(searchHistories.get(0).getUser().getId()).isEqualTo(testUser.getId());
            assertThat(searchHistories.get(0).getQuery()).isEqualTo("Spring Boot");
        }

        @Test
        @DisplayName("legacy searchWord alias 허용")
        void legacySearchWordAliasProvided_ReturnsOk() throws Exception {
            LocalDateTime searchedAt = LocalDateTime.now();
            String requestJson = """
                    {
                      "searchWord": "Spring Boot",
                      "searchedAt": "%s"
                    }
                    """.formatted(searchedAt);

            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            List<SearchHistory> searchHistories = searchHistoryRepository.findAll();
            assertThat(searchHistories).hasSize(1);
            assertThat(searchHistories.get(0).getQuery()).isEqualTo("Spring Boot");
        }

        @Test
        @DisplayName("여러 개 저장")
        void multipleRequests_ReturnsRecentHistories() throws Exception {
            SearchHistoryRequest request1 = new SearchHistoryRequest("Spring Boot", LocalDateTime.now());
            SearchHistoryRequest request2 = new SearchHistoryRequest("Java", LocalDateTime.now());
            SearchHistoryRequest request3 = new SearchHistoryRequest("Kotlin", LocalDateTime.now());

            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request3)))
                    .andExpect(status().isCreated());

            List<SearchHistory> searchHistories = searchHistoryRepository.findAll();
            assertThat(searchHistories).hasSize(3);
        }

        @Test
        @DisplayName("검색어가 비어 있으면 실패한다")
        void blankQuery_ReturnsBadRequest() throws Exception {
            SearchHistoryRequest request = new SearchHistoryRequest("", LocalDateTime.now());

            mockMvc.perform(post("/api/v1/activities/searches")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
