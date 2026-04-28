package com.techfork.domain.user.converter;

import com.techfork.domain.user.dto.AccountProfileResponse;
import com.techfork.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public AccountProfileResponse toAccountProfileResponse(User user) {
        return AccountProfileResponse.builder()
                .profileImage(user.getProfileImage())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .description(user.getDescription())
                .build();
    }
}
