package com.techfork.domain.post.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.techfork.domain.post.entity.Post;
import com.techfork.global.config.StringToLocalDateTimeConverter;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "posts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long postId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private String company;

    @Field(type = FieldType.Keyword)
    private String url;

    @Field(type = FieldType.Keyword, name = "publishedAt")
    @JsonProperty("publishedAt")
    @Getter(AccessLevel.NONE)
    private String publishedAtString;

    @Field(type = FieldType.Dense_Vector, dims = 3072)
    private List<Float> titleEmbedding;

    @Field(type = FieldType.Dense_Vector, dims = 3072)
    private List<Float> summaryEmbedding;

    @Field(type = FieldType.Nested)
    private List<ContentChunk> contentChunks;

    public LocalDateTime getPublishedAt() {
        if (publishedAtString == null) {
            return null;
        }
        return new StringToLocalDateTimeConverter().convert(publishedAtString);
    }

    // Jackson 역직렬화를 위한 setter
    @JsonProperty("publishedAt")
    private void setPublishedAtString(String publishedAtString) {
        this.publishedAtString = publishedAtString;
    }

    public static PostDocument create(Post post, List<Float> titleEmbedding,
                                      List<Float> summaryEmbedding,
                                      List<ContentChunk> contentChunks) {
        return PostDocument.builder()
                .id(String.valueOf(post.getId()))
                .postId(post.getId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .company(post.getCompany())
                .url(post.getUrl())
                .publishedAtString(post.getPublishedAt() != null ? post.getPublishedAt().toString() : null)
                .titleEmbedding(titleEmbedding)
                .summaryEmbedding(summaryEmbedding)
                .contentChunks(contentChunks)
                .build();
    }
}
