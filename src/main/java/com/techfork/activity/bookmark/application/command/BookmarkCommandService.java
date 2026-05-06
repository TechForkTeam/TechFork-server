package com.techfork.activity.bookmark.application.command;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.domain.BookmarkErrorCode;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.service.PostLookupService;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.service.UserLookupService;
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

    private final PostLookupService postLookupService;
    private final UserLookupService userLookupService;
    private final BookmarkRepository bookmarkRepository;

    public void addBookmark(AddBookmarkCommand command) {
        User user = userLookupService.getUserOrThrow(command.userId());
        Post post = postLookupService.getPostOrThrow(command.postId());

        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            throw new GeneralException(BookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        Bookmark bookmark = Bookmark.create(user, post, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        log.info("Saved bookmark for user {} and post {}", command.userId(), command.postId());
    }

    public void deleteBookmark(DeleteBookmarkCommand command) {
        User user = userLookupService.getUserOrThrow(command.userId());
        Post post = postLookupService.getPostOrThrow(command.postId());

        Bookmark bookmark = bookmarkRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new GeneralException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
        log.info("Deleted bookmark for user {} and post {}", command.userId(), command.postId());
    }
}
