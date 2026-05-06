package com.techfork.activity.bookmark.application.query.lookup;

import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkLookupService {

    private final BookmarkRepository bookmarkRepository;

    public Set<Long> getBookmarkedPostIds(Long userId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(bookmarkRepository.findBookmarkedPostIds(userId, postIds));
    }
}
