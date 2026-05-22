package com.techfork.post.domain.projection;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentChunk {

    @Field(type = FieldType.Integer)
    private Integer chunkOrder;

    @Field(type = FieldType.Text)
    private String chunkText;

    @Field(type = FieldType.Dense_Vector, dims = 3072)
    private List<Float> embedding;

    public static ContentChunk create(int order, String text, List<Float> embedding) {
        return ContentChunk.builder()
                .chunkOrder(order)
                .chunkText(text)
                .embedding(embedding)
                .build();
    }
}
