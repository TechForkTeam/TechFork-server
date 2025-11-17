package com.techfork.domain.activity.service;

import com.techfork.domain.activity.dto.BookmarkRequest;
import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.exception.ActivityErrorCode;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityCommandService {

    private final ReadPostRepository readPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ScrabPostRepository scrabPostRepository;

    @Transactional
    public void saveReadPost(Long userId, ReadPostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        if (readPostRepository.existsByUserAndPost(user, post)) {
            log.info("User {} has already read post {}", userId, request.postId());
            return;
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                request.readAt(),
                request.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {}", userId, request.postId());
    }

    @Transactional
    public void saveSearchHistory(Long userId, SearchHistoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        SearchHistory searchHistory = SearchHistory.create(
                user,
                request.searchWord(),
                request.searchedAt()
        );

        searchHistoryRepository.save(searchHistory);
        log.info("Saved search history for user {} with keyword: {}", userId, request.searchWord());
    }

    @Transactional
    public void addBookmark(Long userId, BookmarkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        if (scrabPostRepository.existsByUserAndPost(user, post)) {
            throw new GeneralException(ActivityErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        ScrabPost scrabPost = ScrabPost.create(user, post, LocalDateTime.now());
        scrabPostRepository.save(scrabPost);

        log.info("Saved bookmark for user {} and post {}", userId, request.postId());
    }

    @Transactional
    public void deleteBookmark(Long userId, BookmarkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        ScrabPost scrabPost = scrabPostRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new GeneralException(ActivityErrorCode.BOOKMARK_NOT_FOUND));

        scrabPostRepository.delete(scrabPost);
        log.info("Deleted bookmark for user {} and post {}", userId, request.postId());
    }

}
