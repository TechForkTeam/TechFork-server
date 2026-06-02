package com.techfork.useraccount.domain;

import com.techfork.global.common.BaseTimeEntity;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
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

    private String profileImage;

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
    User(String nickName, String email, String profileImage, String description, SocialType socialType, String socialId, Role role, UserStatus status) {
        this.nickName = nickName;
        this.email = email;
        this.profileImage = profileImage;
        this.description = description;
        this.socialType = socialType;
        this.socialId = socialId;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : UserStatus.PENDING;
    }

    public static User createSocialUser(SocialType socialType, String socialId, String email, String profileImage) {
        return User.builder()
                .socialType(socialType)
                .socialId(socialId)
                .email(email)
                .profileImage(profileImage)
                .role(Role.USER)
                .build();
    }

    public void updateUser(String nickName, String email, String description) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
        this.status = UserStatus.ACTIVE;
    }

    public void updateProfile(String nickName, String description) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void replaceInterests(List<UserInterestSelection> interests) {
        List<UserInterestCategory> newInterestCategories = interests.stream()
                .map(this::createInterestCategory)
                .toList();

        this.interestCategories.clear();
        this.interestCategories.addAll(newInterestCategories);
    }

    private UserInterestCategory createInterestCategory(UserInterestSelection interest) {
        UserInterestCategory userInterestCategory = UserInterestCategory.create(this, interest.category());

        if (interest.keywords() != null && !interest.keywords().isEmpty()) {
            addKeywordsToCategory(userInterestCategory, interest);
        }

        return userInterestCategory;
    }

    private void addKeywordsToCategory(UserInterestCategory userInterestCategory, UserInterestSelection interest) {
        interest.keywords().forEach(keyword -> {
            validateKeywordCategory(keyword, interest.category());
            userInterestCategory.addKeyword(UserInterestKeyword.create(userInterestCategory, keyword));
        });
    }

    private void validateKeywordCategory(EInterestKeyword keyword, EInterestCategory category) {
        if (keyword.getCategory() != category) {
            throw new GeneralException(UserErrorCode.INVALID_INTEREST_KEYWORD);
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isWithdrawn() {
        return status == UserStatus.WITHDRAWN;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.nickName = null;
        this.email = null;
        this.profileImage = null;
        this.description = null;
    }

    public void reactivate(String email, String profileImage) {
        this.email = email;
        this.profileImage = profileImage;
        this.status = UserStatus.PENDING;
    }
}
