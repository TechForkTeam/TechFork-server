package com.techfork.activity.readhistory.domain;

import com.techfork.useraccount.domain.User;
import com.techfork.global.common.BaseEntity;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

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
