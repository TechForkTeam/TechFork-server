package com.techfork.post.application.query.lookup;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;
import com.techfork.post.infrastructure.PostKeywordRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostKeywordLookupServiceTest {

    @Mock
    private PostKeywordRepository postKeywordRepository;

    @InjectMocks
    private PostKeywordLookupService postKeywordLookupService;

    @Test
    @DisplayName("입력 postIds가 비어있으면 repository 호출 없이 빈 Map을 반환한다")
    void getKeywordsByPostIds_EmptyInput_ReturnsEmptyMap() {
        Map<Long, List<String>> result = postKeywordLookupService.getKeywordsByPostIds(List.of());

        assertThat(result).isEmpty();
        verify(postKeywordRepository, never()).findByPostIdIn(any());
    }

    @Test
    @DisplayName("postId별 keyword map을 반환한다")
    void getKeywordsByPostIds_ReturnsKeywordMap() {
        Post post1 = mock(Post.class);
        Post post2 = mock(Post.class);
        given(post1.getId()).willReturn(101L);
        given(post2.getId()).willReturn(102L);
        given(postKeywordRepository.findByPostIdIn(List.of(101L, 102L))).willReturn(List.of(
                PostKeyword.builder().keyword("Java").post(post1).build(),
                PostKeyword.builder().keyword("Spring").post(post1).build(),
                PostKeyword.builder().keyword("Kotlin").post(post2).build()
        ));

        Map<Long, List<String>> result = postKeywordLookupService.getKeywordsByPostIds(List.of(101L, 102L));

        assertThat(result.get(101L)).containsExactlyInAnyOrder("Java", "Spring");
        assertThat(result.get(102L)).containsExactly("Kotlin");
    }
}
