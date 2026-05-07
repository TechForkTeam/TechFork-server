package com.techfork.domain.post.service;

import com.techfork.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostCommandService postCommandService;

    @Test
    @DisplayName("incrementViewCount - repository atomic update를 위임한다")
    void incrementViewCount_DelegatesToRepositoryAtomicUpdate() {
        Long postId = 100L;

        postCommandService.incrementViewCount(postId);

        verify(postRepository, times(1)).incrementViewCount(postId);
    }
}
