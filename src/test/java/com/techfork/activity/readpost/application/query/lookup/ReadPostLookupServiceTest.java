package com.techfork.activity.readpost.application.query.lookup;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadPostLookupServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;

    @InjectMocks
    private ReadPostLookupService readPostLookupService;

    @Test
    @DisplayName("최근 읽은 게시글 활동 신호를 조회한다")
    void getRecentReadPostActivities_ReturnsReadPostActivities() {
        Long userId = 1L;
        int limit = 20;
        ReadPost readPost = readPost("읽은 포스트", List.of("Java", "Spring"), 120);
        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, limit)))
                .willReturn(List.of(readPost));

        List<ReadPostLookupItem> result = readPostLookupService.getRecentReadPostActivities(userId, limit);

        assertThat(result).containsExactly(new ReadPostLookupItem("읽은 포스트", List.of("Java", "Spring"), 120));
        verify(readPostRepository).findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, limit));
    }

    private ReadPost readPost(String title, List<String> keywords, Integer readDurationSeconds) {
        ReadPost readPost = mock(ReadPost.class);
        Post post = mock(Post.class);
        List<PostKeyword> postKeywords = keywords.stream()
                .map(this::postKeyword)
                .toList();
        given(readPost.getPost()).willReturn(post);
        given(readPost.getReadDurationSeconds()).willReturn(readDurationSeconds);
        given(post.getTitle()).willReturn(title);
        given(post.getKeywords()).willReturn(postKeywords);
        return readPost;
    }

    private PostKeyword postKeyword(String keyword) {
        PostKeyword postKeyword = mock(PostKeyword.class);
        given(postKeyword.getKeyword()).willReturn(keyword);
        return postKeyword;
    }
}
