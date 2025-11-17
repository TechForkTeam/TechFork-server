package com.techfork.domain.activity.service;

import com.techfork.domain.activity.converter.ActivityConverter;
import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityQueryService {

    private final UserRepository userRepository;
    private final ScrabPostRepository scrabPostRepository;
    private final ActivityConverter activityConverter;

    public BookmarkListResponse getBookmarks(Long userId, Long lastBookmarkId, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<BookmarkDto> bookmarks = scrabPostRepository.findBookmarksWithCursor(user, lastBookmarkId, pageRequest);

        return activityConverter.toBookmarkListResponse(bookmarks, size);
    }
}
