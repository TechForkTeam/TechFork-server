package com.techfork.activity.readpost.domain;

import com.techfork.activity.readpost.infrastructure.FirstReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.domain.useraccount.entity.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadPostFirstReadPolicy {

    private final FirstReadPostRepository firstReadPostRepository;

    public boolean markFirstRead(User user, Post post, LocalDateTime readAt) {
        return firstReadPostRepository.markFirstRead(user.getId(), post.getId(), readAt);
    }
}
