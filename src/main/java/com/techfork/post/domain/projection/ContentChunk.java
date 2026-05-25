package com.techfork.post.domain.projection;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * 검색/추천용 PostDocument 내부에 포함되는 콘텐츠 청크 projection.
 *
 * <p>ContentChunk 는 RDB aggregate 나 독립 도메인 엔티티가 아니라, Elasticsearch read model
 * 안에서만 사용하는 projection 값이다.</p>
 */
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
