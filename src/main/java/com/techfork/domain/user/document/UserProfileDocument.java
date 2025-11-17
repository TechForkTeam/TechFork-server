package com.techfork.domain.user.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileDocument {

    @Id
    private String id;  // userId를 문자열로

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text)
    private String profileText;

    @Field(type = FieldType.Dense_Vector, dims = 3072)  // OpenAI text-embedding-3-large dimension
    private float[] profileVector;

    @Field(type = FieldType.Keyword)
    private List<String> interests;

    @Field(type = FieldType.Keyword)
    private List<String> preferredTopics;

    @Field(type = FieldType.Date)
    private LocalDateTime generatedAt;

    @Builder
    private UserProfileDocument(Long userId, String profileText, float[] profileVector,
                                 List<String> interests, List<String> preferredTopics, LocalDateTime generatedAt) {
        this.id = String.valueOf(userId);
        this.userId = userId;
        this.profileText = profileText;
        this.profileVector = profileVector;
        this.interests = interests;
        this.preferredTopics = preferredTopics;
        this.generatedAt = generatedAt;
    }

    public static UserProfileDocument create(Long userId, String profileText, float[] profileVector,
                                              List<String> interests, List<String> preferredTopics) {
        return UserProfileDocument.builder()
                .userId(userId)
                .profileText(profileText)
                .profileVector(profileVector)
                .interests(interests)
                .preferredTopics(preferredTopics)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
