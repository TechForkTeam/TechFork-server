package com.techfork.domain.activity.entity;

import com.techfork.domain.user.entity.User;
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
    private String searchWord;

    private LocalDateTime searchedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PersistenceCreator
    @Builder
    SearchHistory(User user, String searchWord, LocalDateTime searchedAt) {
        this.user = user;
        this.searchWord = searchWord;
        this.searchedAt = searchedAt;
    }

    public static SearchHistory create(User user, String searchWord, LocalDateTime searchedAt) {
        return SearchHistory.builder()
                .user(user)
                .searchWord(searchWord)
                .searchedAt(searchedAt)
                .build();
    }
}
