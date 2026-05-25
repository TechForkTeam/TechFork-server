package com.techfork.post.application.query.lookup;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.exception.PostErrorCode;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * Activity 같은 다른 컨텍스트가 Post repository 직접 의존 없이 Post aggregate를 조회할 때 사용하는
 * 현재 phase의 임시 application seam.
 *
 * <p>이 서비스는 Post repository를 외부 컨텍스트에 직접 노출하지 않기 위한
 * repository-access choke-point이며, 아직은 Post aggregate 자체를 반환한다.
 * 진짜 published query contract 가 되려면 후속 단계에서 DTO/port 로 더 좁혀야 한다.</p>
 */
public class PostLookupService {

    private final PostRepository postRepository;

    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));
    }
}
