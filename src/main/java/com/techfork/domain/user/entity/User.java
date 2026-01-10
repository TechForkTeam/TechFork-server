package com.techfork.domain.user.entity;

import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class User extends BaseTimeEntity {

    private String nickName;

    @Column(unique = true)
    private String email;

    private String description;

    @OneToMany
    @JoinColumn(name = "user_id")
    private List<UserInterestCategory> interestCategories;

    public void updateUser(String nickName, String email, String description) {
        this.nickName = nickName;
        this.email = email;
        this.description = description;
    }
}
