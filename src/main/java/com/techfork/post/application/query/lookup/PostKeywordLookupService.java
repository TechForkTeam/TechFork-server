package com.techfork.post.application.query.lookup;

import com.techfork.post.domain.PostKeyword;
import com.techfork.post.infrastructure.PostKeywordRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * Activity 와 Post query 조합 로직이 게시글 키워드를 읽기 전용으로 가져갈 때 사용하는
 * 현재 phase의 임시 application seam.
 *
 * <p>키워드 조회를 PostKeywordRepository 직접 의존에서 분리해, 다른 컨텍스트가
 * application seam을 통해 게시글 메타데이터를 소비하도록 고정한다.
 * 다만 이것도 아직은 Post 컨텍스트 내부 모델에 밀접한 조회 seam 이며, 별도 published query 로
 * 안정화된 상태는 아니다.</p>
 */
public class PostKeywordLookupService {

    private final PostKeywordRepository postKeywordRepository;

    public Map<Long, List<String>> getKeywordsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        postKeyword -> postKeyword.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));
    }
}
