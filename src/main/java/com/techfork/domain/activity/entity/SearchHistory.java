package com.techfork.domain.activity.entity;

import com.techfork.domain.useraccount.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String query;

    private LocalDateTime searchedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PersistenceCreator
    @Builder
    SearchHistory(User user, String query, LocalDateTime searchedAt) {
        this.user = user;
        this.query = query;
        this.searchedAt = searchedAt;
    }

    public static SearchHistory create(User user, String query, LocalDateTime searchedAt) {
        return SearchHistory.builder()
                .user(user)
                .query(query)
                .searchedAt(searchedAt)
                .build();
    }
}
