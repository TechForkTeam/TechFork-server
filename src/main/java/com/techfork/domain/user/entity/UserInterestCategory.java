package com.techfork.domain.user.entity;

import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_interest_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterestCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EInterestCategory category;

    @OneToMany(mappedBy = "userInterestCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterestKeyword> keywords = new ArrayList<>();

    @Builder
    private UserInterestCategory(User user, EInterestCategory category) {
        this.user = user;
        this.category = category;
    }

    public static UserInterestCategory create(User user, EInterestCategory category) {
        return UserInterestCategory.builder()
                .user(user)
                .category(category)
                .build();
    }

    public void addKeyword(UserInterestKeyword keyword) {
        this.keywords.add(keyword);
        keyword.setUserInterestCategory(this);
    }
}
