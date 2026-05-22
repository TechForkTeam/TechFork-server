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
