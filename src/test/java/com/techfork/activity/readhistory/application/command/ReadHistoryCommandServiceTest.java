package com.techfork.activity.readhistory.application.command;

import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.global.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserLookupService userLookupService;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private ReadHistoryCommandService readHistoryCommandService;

    @Nested
    @DisplayName("검색 히스토리 저장")
    class SaveSearchHistory {

        @Test
        @DisplayName("검색 히스토리 저장 성공")
        void validCommand_SavesSearchHistory() {
            Long userId = 1L;
            String query = "Spring Boot";
            LocalDateTime searchedAt = LocalDateTime.now();
            SaveSearchHistoryCommand command = new SaveSearchHistoryCommand(userId, query, searchedAt);

            User mockUser = mock(User.class);
            given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
            given(searchHistoryRepository.save(any(SearchHistory.class))).willReturn(mock(SearchHistory.class));

            readHistoryCommandService.saveSearchHistory(command);

            verify(userLookupService, times(1)).getUserOrThrow(userId);
            ArgumentCaptor<SearchHistory> searchHistoryCaptor = ArgumentCaptor.forClass(SearchHistory.class);
            verify(searchHistoryRepository, times(1)).save(searchHistoryCaptor.capture());
            assertThat(searchHistoryCaptor.getValue().getQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("존재하지 않는 사용자")
        void userNotFound_ThrowsUserNotFound() {
            Long userId = 999L;
            SaveSearchHistoryCommand command = new SaveSearchHistoryCommand(userId, "Spring Boot", LocalDateTime.now());

            given(userLookupService.getUserOrThrow(userId))
                    .willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> readHistoryCommandService.saveSearchHistory(command))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

            verify(searchHistoryRepository, never()).save(any());
        }
    }
}
