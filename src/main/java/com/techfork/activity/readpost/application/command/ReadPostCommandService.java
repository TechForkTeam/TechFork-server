package com.techfork.activity.readpost.application.command;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.domain.ReadPostErrorCode;
import com.techfork.activity.readpost.domain.ReadPostFirstReadPolicy;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.application.command.PostViewCountCommandService;
import com.techfork.post.application.query.lookup.PostLookupService;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.service.UserLookupService;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReadPostCommandService {

    private final ReadPostRepository readPostRepository;
    private final PostLookupService postLookupService;
    private final PostViewCountCommandService postViewCountCommandService;
    private final UserLookupService userLookupService;
    private final ReadPostFirstReadPolicy readPostFirstReadPolicy;

    public void saveReadPost(SaveReadPostCommand command) {
        User user = userLookupService.getUserOrThrow(command.userId());
        Post post = postLookupService.getPostOrThrow(command.postId());

        boolean firstReadMarked = readPostFirstReadPolicy.markFirstRead(user, post, command.readAt());
        boolean viewCountIncremented = false;
        if (firstReadMarked) {
            viewCountIncremented = postViewCountCommandService.incrementViewCount(post.getId());
            if (!viewCountIncremented) {
                throw new GeneralException(ReadPostErrorCode.READ_POST_VIEW_COUNT_INCREMENT_FAILED);
            }
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                command.readAt(),
                command.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {} (firstReadMarked: {}, viewCount incremented: {})",
                command.userId(), command.postId(), firstReadMarked, viewCountIncremented);
    }
}
