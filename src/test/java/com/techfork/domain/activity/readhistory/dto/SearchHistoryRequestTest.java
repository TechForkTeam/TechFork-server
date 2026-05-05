package com.techfork.domain.activity.readhistory.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SearchHistoryRequestTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Test
    @DisplayName("검색 히스토리 요청은 query 필드로 역직렬화된다")
    void deserialize_WithQueryField() throws Exception {
        String requestJson = """
                {
                  "query": "Spring Boot",
                  "searchedAt": "2026-04-24T16:30:00"
                }
                """;

        SearchHistoryRequest request = objectMapper.readValue(requestJson, SearchHistoryRequest.class);

        assertThat(request.query()).isEqualTo("Spring Boot");
        assertThat(request.searchedAt()).isEqualTo(LocalDateTime.of(2026, 4, 24, 16, 30));
    }

    @Test
    @DisplayName("검색 히스토리 요청은 legacy searchWord alias도 허용한다")
    void deserialize_WithLegacySearchWordAlias() throws Exception {
        String requestJson = """
                {
                  "searchWord": "Spring Boot",
                  "searchedAt": "2026-04-24T16:30:00"
                }
                """;

        SearchHistoryRequest request = objectMapper.readValue(requestJson, SearchHistoryRequest.class);

        assertThat(request.query()).isEqualTo("Spring Boot");
    }
}
