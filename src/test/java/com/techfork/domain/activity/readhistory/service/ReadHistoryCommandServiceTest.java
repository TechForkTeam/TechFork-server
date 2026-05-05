package com.techfork.domain.activity.readhistory.service;

import com.techfork.domain.activity.readhistory.dto.SearchHistoryRequest;
import com.techfork.domain.activity.readhistory.entity.SearchHistory;
import com.techfork.domain.activity.readhistory.repository.SearchHistoryRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadHistoryCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private ReadHistoryCommandService readHistoryCommandService;

    @Nested
    @DisplayName("검색 히스토리 저장")
    class SaveSearchHistory {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("검색 히스토리 저장 성공")
            void saveSearchHistory_Success() {
                Long userId = 1L;
                String query = "Spring Boot";
                LocalDateTime searchedAt = LocalDateTime.now();
                SearchHistoryRequest request = new SearchHistoryRequest(query, searchedAt);

                User mockUser = mock(User.class);
                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(searchHistoryRepository.save(any(SearchHistory.class))).willReturn(mock(SearchHistory.class));

                readHistoryCommandService.saveSearchHistory(userId, request);

                verify(userRepository, times(1)).findById(userId);
                ArgumentCaptor<SearchHistory> searchHistoryCaptor = ArgumentCaptor.forClass(SearchHistory.class);
                verify(searchHistoryRepository, times(1)).save(searchHistoryCaptor.capture());
                assertThat(searchHistoryCaptor.getValue().getQuery()).isEqualTo(query);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 사용자")
            void saveSearchHistory_Fail_UserNotFound() {
                Long userId = 999L;
                SearchHistoryRequest request = new SearchHistoryRequest("Spring Boot", LocalDateTime.now());

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> readHistoryCommandService.saveSearchHistory(userId, request))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

                verify(searchHistoryRepository, never()).save(any());
            }
        }
    }
}
