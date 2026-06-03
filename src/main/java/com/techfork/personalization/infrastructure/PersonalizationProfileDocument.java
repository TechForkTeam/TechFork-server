package com.techfork.personalization.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalizationProfileDocument {

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
    private List<String> keyKeywords;

    @Field(type = FieldType.Date)
    @Transient
    private LocalDateTime generatedAt;

    @Builder
    private PersonalizationProfileDocument(Long userId, String profileText, float[] profileVector,
                                 List<String> interests, List<String> keyKeywords, LocalDateTime generatedAt) {
        this.id = String.valueOf(userId);
        this.userId = userId;
        this.profileText = profileText;
        this.profileVector = profileVector;
        this.interests = interests;
        this.keyKeywords = keyKeywords;
        this.generatedAt = generatedAt;
    }

    public static PersonalizationProfileDocument create(Long userId, String profileText, float[] profileVector,
                                              List<String> interests, List<String> keyKeywords) {
        return PersonalizationProfileDocument.builder()
                .userId(userId)
                .profileText(profileText)
                .profileVector(profileVector)
                .interests(interests)
                .keyKeywords(keyKeywords)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
