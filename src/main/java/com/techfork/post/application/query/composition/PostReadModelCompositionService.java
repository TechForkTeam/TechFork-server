package com.techfork.post.application.query.composition;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.post.application.query.lookup.PostKeywordLookupService;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReadModelCompositionService {

    private final PostKeywordLookupService postKeywordLookupService;
    private final BookmarkLookupService bookmarkLookupService;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public List<PostInfoRow> composePostInfoRows(List<PostInfoRow> posts, Long userId) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
                .map(PostInfoRow::id)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordLookupService.getKeywordsByPostIds(postIds);
        Set<Long> bookmarkedPostIds = userId == null
                ? Set.of()
                : bookmarkLookupService.getBookmarkedPostIds(userId, postIds);

        return posts.stream()
                .map(post -> post.toBuilder()
                        .thumbnailUrl(thumbnailOptimizer.optimize(post.thumbnailUrl()))
                        .keywords(keywordMap.getOrDefault(post.id(), List.of()))
                        .isBookmarked(resolveBookmarkStatus(userId, bookmarkedPostIds, post.id()))
                        .build())
                .toList();
    }

    public GetPostDetailResult composePostDetail(PostDetailRow postDetail, Long userId) {
        Long postId = postDetail.id();
        List<String> keywords = postKeywordLookupService.getKeywordsByPostIds(List.of(postId))
                .getOrDefault(postId, List.of());
        Boolean isBookmarked = resolveBookmarkStatus(
                userId,
                userId == null ? Set.of() : bookmarkLookupService.getBookmarkedPostIds(userId, List.of(postId)),
                postId
        );

        return GetPostDetailResult.builder()
                .id(postDetail.id())
                .title(postDetail.title())
                .summary(postDetail.summary())
                .company(postDetail.company())
                .url(postDetail.url())
                .logoUrl(postDetail.logoUrl())
                .publishedAt(postDetail.publishedAt())
                .viewCount(postDetail.viewCount())
                .keywords(keywords)
                .isBookmarked(isBookmarked)
                .build();
    }

    private Boolean resolveBookmarkStatus(Long userId, Set<Long> bookmarkedPostIds, Long postId) {
        if (userId == null) {
            return null;
        }
        return bookmarkedPostIds.contains(postId);
    }
}
