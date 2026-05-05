package com.techfork.activity.bookmark.application.command;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.domain.BookmarkErrorCode;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
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

    public void addBookmark(AddBookmarkCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            throw new GeneralException(BookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        Bookmark bookmark = Bookmark.create(user, post, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        log.info("Saved bookmark for user {} and post {}", command.userId(), command.postId());
    }

    public void deleteBookmark(DeleteBookmarkCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        Bookmark bookmark = bookmarkRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new GeneralException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
        log.info("Deleted bookmark for user {} and post {}", command.userId(), command.postId());
    }
}
