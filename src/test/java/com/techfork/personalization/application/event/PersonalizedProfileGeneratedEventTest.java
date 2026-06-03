package com.techfork.personalization.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalizedProfileGeneratedEventTest {

    @Test
    @DisplayName("프로필 벡터는 생성 시점 값으로 복사된다")
    void constructor_CopiesProfileVector() {
        float[] profileVector = new float[]{0.1f, 0.2f};

        PersonalizedProfileGeneratedEvent event = new PersonalizedProfileGeneratedEvent(
                1L,
                profileVector,
                List.of("Spring")
        );
        profileVector[0] = 9.9f;

        assertThat(event.profileVector()).containsExactly(0.1f, 0.2f);
    }

    @Test
    @DisplayName("프로필 벡터 조회 결과를 변경해도 이벤트 내부 상태는 변경되지 않는다")
    void profileVector_ReturnsCopy() {
        PersonalizedProfileGeneratedEvent event = new PersonalizedProfileGeneratedEvent(
                1L,
                new float[]{0.1f, 0.2f},
                List.of("Spring")
        );

        float[] returnedVector = event.profileVector();
        returnedVector[0] = 9.9f;

        assertThat(event.profileVector()).containsExactly(0.1f, 0.2f);
    }

    @Test
    @DisplayName("핵심 키워드는 생성 시점 값으로 복사되고 불변 목록으로 유지된다")
    void constructor_CopiesKeyKeywordsAsImmutableList() {
        List<String> keyKeywords = new ArrayList<>(List.of("Spring", "JPA"));

        PersonalizedProfileGeneratedEvent event = new PersonalizedProfileGeneratedEvent(
                1L,
                new float[]{0.1f},
                keyKeywords
        );
        keyKeywords.add("Kafka");

        assertThat(event.keyKeywords()).containsExactly("Spring", "JPA");
        assertThatThrownBy(() -> event.keyKeywords().add("Kafka"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("핵심 키워드가 null이면 빈 목록으로 정규화한다")
    void constructor_NullKeyKeywordsDefaultsToEmptyList() {
        PersonalizedProfileGeneratedEvent event = new PersonalizedProfileGeneratedEvent(
                1L,
                new float[]{0.1f},
                null
        );

        assertThat(event.keyKeywords()).isEmpty();
    }

    @Test
    @DisplayName("프로필 벡터가 null이면 null로 유지한다")
    void constructor_NullProfileVectorRemainsNull() {
        PersonalizedProfileGeneratedEvent event = new PersonalizedProfileGeneratedEvent(
                1L,
                null,
                List.of("Spring")
        );

        assertThat(event.profileVector()).isNull();
    }
}
