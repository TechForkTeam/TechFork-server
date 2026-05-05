package com.techfork.domain.activity.service;

import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityCommandService {

    private final ReadPostRepository readPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void saveReadPost(Long userId, ReadPostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        boolean isFirstRead = !readPostRepository.existsByUserAndPost(user, post);
        if (isFirstRead) {
            post.incrementViewCount();
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                request.readAt(),
                request.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {} (viewCount incremented: {})",
                userId, request.postId(), isFirstRead);
    }

    @Transactional
    public void saveSearchHistory(Long userId, SearchHistoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        SearchHistory searchHistory = SearchHistory.create(
                user,
                request.query(),
                request.searchedAt()
        );

        searchHistoryRepository.save(searchHistory);
        log.info("Saved search history for user {} with query: {}", userId, request.query());
    }

}
