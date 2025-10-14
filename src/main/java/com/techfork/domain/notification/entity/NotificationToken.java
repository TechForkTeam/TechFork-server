package com.techfork.domain.notification.entity;

import com.techfork.domain.user.entity.User;
import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationToken extends BaseTimeEntity {

    @Column(nullable = false, length = 500)
    private String token;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
