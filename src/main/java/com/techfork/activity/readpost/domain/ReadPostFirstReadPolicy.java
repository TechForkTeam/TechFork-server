package com.techfork.activity.readpost.domain;

import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadPostFirstReadPolicy {

    private final ReadPostRepository readPostRepository;

    public boolean isFirstRead(User user, Post post) {
        return !readPostRepository.existsByUserAndPost(user, post);
    }
}
