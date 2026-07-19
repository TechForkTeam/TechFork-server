package com.techfork.activity.readpost.application.query.lookup;

import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.PostKeyword;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadPostLookupService {

    private final ReadPostRepository readPostRepository;

    public Set<Long> getRecentReadPostIds(Long userId, int limit) {
        return readPostRepository.findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, limit))
                .stream()
                .map(readPost -> readPost.getPost().getId())
                .collect(Collectors.toSet());
    }

    public List<ReadPostLookupItem> getRecentReadPostActivities(Long userId, int limit) {
        return readPostRepository.findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, limit))
                .stream()
                .map(readPost -> new ReadPostLookupItem(
                        readPost.getPost().getTitle(),
                        readPost.getPost().getKeywords().stream()
                                .map(PostKeyword::getKeyword)
                                .toList(),
                        readPost.getReadDurationSeconds()
                ))
                .toList();
    }
}
