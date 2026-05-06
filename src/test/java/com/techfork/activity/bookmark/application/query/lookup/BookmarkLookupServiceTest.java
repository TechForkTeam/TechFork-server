package com.techfork.activity.bookmark.application.query.lookup;

import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkLookupServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkLookupService bookmarkLookupService;

    @Nested
    @DisplayName("북마크된 게시글 ID 조회")
    class GetBookmarkedPostIds {

        @Test
        @DisplayName("입력 postIds가 비어있으면 repository 호출 없이 빈 Set을 반환한다")
        void getBookmarkedPostIds_EmptyInput_ReturnsEmptySet() {
            Set<Long> result = bookmarkLookupService.getBookmarkedPostIds(1L, List.of());

            assertThat(result).isEmpty();
            verify(bookmarkRepository, never()).findBookmarkedPostIds(anyLong(), any());
        }

        @Test
        @DisplayName("repository 조회 결과를 Set으로 변환해 반환한다")
        void getBookmarkedPostIds_ReturnsRepositoryResultAsSet() {
            Long userId = 1L;
            List<Long> postIds = List.of(101L, 102L, 103L);
            given(bookmarkRepository.findBookmarkedPostIds(userId, postIds)).willReturn(List.of(101L, 103L));

            Set<Long> result = bookmarkLookupService.getBookmarkedPostIds(userId, postIds);

            assertThat(result).containsExactlyInAnyOrder(101L, 103L);
        }
    }
}
