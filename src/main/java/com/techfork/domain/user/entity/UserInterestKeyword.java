package com.techfork.domain.user.entity;

import com.techfork.domain.user.enums.EInterestKeyword;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_interest_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterestKeyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_interest_category_id", nullable = false)
    private UserInterestCategory userInterestCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EInterestKeyword keyword;

    @Builder
    private UserInterestKeyword(UserInterestCategory userInterestCategory, EInterestKeyword keyword) {
        this.userInterestCategory = userInterestCategory;
        this.keyword = keyword;
    }

    public static UserInterestKeyword create(UserInterestCategory userInterestCategory, EInterestKeyword keyword) {
        return UserInterestKeyword.builder()
                .userInterestCategory(userInterestCategory)
                .keyword(keyword)
                .build();
    }

    protected void setUserInterestCategory(UserInterestCategory userInterestCategory) {
        this.userInterestCategory = userInterestCategory;
    }
}
