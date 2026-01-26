package com.techfork.domain.user.converter;

import com.techfork.domain.user.dto.UserProfileResponse;
import com.techfork.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .profileImage(user.getProfileImage())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .description(user.getDescription())
                .build();
    }
}
