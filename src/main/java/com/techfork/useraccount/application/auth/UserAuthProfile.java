package com.techfork.useraccount.application.auth;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;

public record UserAuthProfile(
        Long id,
        Role role,
        UserStatus status,
        String email,
        boolean active
) {

    public static UserAuthProfile from(User user) {
        return new UserAuthProfile(
                user.getId(),
                user.getRole(),
                user.getStatus(),
                user.getEmail(),
                user.isActive()
        );
    }
}
