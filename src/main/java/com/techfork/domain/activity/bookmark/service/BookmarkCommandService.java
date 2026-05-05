package com.techfork.domain.activity.bookmark.service;

import com.techfork.domain.activity.dto.BookmarkRequest;
import com.techfork.domain.activity.entity.Bookmark;
import com.techfork.domain.activity.exception.ActivityErrorCode;
import com.techfork.domain.activity.repository.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkCommandService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;

    public void addBookmark(Long userId, BookmarkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            throw new GeneralException(ActivityErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        Bookmark bookmark = Bookmark.create(user, post, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        log.info("Saved bookmark for user {} and post {}", userId, request.postId());
    }

    public void deleteBookmark(Long userId, BookmarkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        Bookmark bookmark = bookmarkRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new GeneralException(ActivityErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
        log.info("Deleted bookmark for user {} and post {}", userId, request.postId());
    }
}
