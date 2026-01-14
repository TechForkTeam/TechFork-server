package com.techfork.domain.user.entity;

import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.enums.UserStatus;
import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"social_type", "social_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private String nickName;

    private String email;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterestCategory> interestCategories = new ArrayList<>();

    @PersistenceCreator
    @Builder
    User(String nickName, String email, String description, SocialType socialType, String socialId, Role role, UserStatus status) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
        this.socialType = socialType;
        this.socialId = socialId;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : UserStatus.PENDING;
    }

    public static User create() {
        return User.builder()
                .build();
    }

    public static User createSocialUser(SocialType socialType, String socialId, String email) {
        return User.builder()
                .socialType(socialType)
                .socialId(socialId)
                .email(email)
                .role(Role.USER)
                .build();
    }

    public void updateUser(String nickName, String email, String description) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
        this.status = UserStatus.ACTIVE;
    }
}
