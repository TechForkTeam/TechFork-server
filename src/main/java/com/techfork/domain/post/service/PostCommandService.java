package com.techfork.domain.post.service;

import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private final PostRepository postRepository;

    /**
     * DB atomic update로 조회수를 증가시킨다.
     *
     * <p>이 메서드는 현재 영속성 컨텍스트에 이미 로드된 {@code Post} 엔티티의
     * {@code viewCount} 값을 동기화하지 않는다. 호출자는 같은 트랜잭션 안에서
     * 기존 managed {@code Post}의 {@code viewCount}가 최신 상태라고 가정하면 안 된다.</p>
     *
     * @return 정확히 1건이 증가되면 {@code true}, 아니면 {@code false}
     */
    public boolean incrementViewCount(Long postId) {
        int updatedCount = postRepository.incrementViewCount(postId);

        if (updatedCount != 1) {
            log.warn("Atomic viewCount increment affected {} rows for postId={}", updatedCount, postId);
            return false;
        }

        log.debug("Atomic viewCount increment applied for postId={}", postId);
        return true;
    }
}
