package com.techfork.domain.user.entity;

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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private String nickName;

    @Column(unique = true)
    private String email;

    private String description;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterestCategory> interestCategories = new ArrayList<>();

    @PersistenceCreator
    @Builder
    User(String nickName, String email, String description) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
    }

    public static User create() {
        return User.builder()
                .build();
    }

    public void updateUser(String nickName, String email, String description) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
    }
}
