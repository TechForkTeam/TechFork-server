package com.techfork.useraccount.converter;

import com.techfork.useraccount.dto.AccountProfileResponse;
import com.techfork.useraccount.entity.User;
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
