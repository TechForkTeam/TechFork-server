package com.techfork.activity.readpost.application.query;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.activity.readpost.infrastructure.ReadPostQueryRow;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.application.query.PostKeywordLookupService;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadPostQueryService {

    private final BookmarkLookupService bookmarkLookupService;
    private final PostKeywordLookupService postKeywordLookupService;
    private final ReadPostRepository readPostRepository;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public GetReadPostsResult getReadPosts(GetReadPostsQuery query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<ReadPostQueryRow> rows = readPostRepository.findReadPostsWithCursor(
                query.userId(),
                query.lastReadPostId(),
                pageRequest
        );
        List<ReadPostItem> readPosts = attachMetadata(rows, query.userId());

        return toGetReadPostsResult(readPosts, query.size());
    }

    private List<ReadPostItem> attachMetadata(List<ReadPostQueryRow> rows, Long userId) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = rows.stream()
                .map(ReadPostQueryRow::postId)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordLookupService.getKeywordsByPostIds(postIds);

        var bookmarkedPostIds = bookmarkLookupService.getBookmarkedPostIds(userId, postIds);

        return rows.stream()
                .map(row -> ReadPostItem.builder()
                        .readPostId(row.readPostId())
                        .postId(row.postId())
                        .title(row.title())
                        .shortSummary(row.shortSummary())
                        .url(row.url())
                        .companyName(row.companyName())
                        .logoUrl(row.logoUrl())
                        .publishedAt(row.publishedAt())
                        .thumbnailUrl(thumbnailOptimizer.optimize(row.thumbnailUrl()))
                        .viewCount(row.viewCount())
                        .keywords(keywordMap.getOrDefault(row.postId(), List.of()))
                        .isBookmarked(bookmarkedPostIds.contains(row.postId()))
                        .readAt(row.readAt())
                        .build())
                .toList();
    }

    private GetReadPostsResult toGetReadPostsResult(List<ReadPostItem> readPosts, int requestedSize) {
        boolean hasNext = readPosts.size() > requestedSize;
        List<ReadPostItem> content = hasNext ? readPosts.subList(0, requestedSize) : readPosts;
        Long lastReadPostId = content.isEmpty() ? null : content.get(content.size() - 1).readPostId();

        return new GetReadPostsResult(content, lastReadPostId, hasNext);
    }
}
