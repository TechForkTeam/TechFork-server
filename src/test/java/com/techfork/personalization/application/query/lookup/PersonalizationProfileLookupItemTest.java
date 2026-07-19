package com.techfork.personalization.application.query.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PersonalizationProfileLookupItemTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("프로필 벡터는 생성 시점 값으로 복사된다")
        void profileVectorProvided_CopiesProfileVector() {
            float[] profileVector = new float[]{0.1f, 0.2f};

            PersonalizationProfileLookupItem item = new PersonalizationProfileLookupItem(
                    profileVector,
                    List.of("Spring")
            );
            profileVector[0] = 9.9f;

            assertThat(item.profileVector()).containsExactly(0.1f, 0.2f);
        }

        @Test
        @DisplayName("핵심 키워드는 생성 시점 값으로 복사되고 불변 목록으로 유지된다")
        void keyKeywordsProvided_CopiesAsImmutableList() {
            List<String> keyKeywords = new ArrayList<>(List.of("Spring", "JPA"));

            PersonalizationProfileLookupItem item = new PersonalizationProfileLookupItem(
                    new float[]{0.1f},
                    keyKeywords
            );
            keyKeywords.add("Kafka");

            assertThat(item.keyKeywords()).containsExactly("Spring", "JPA");
            assertThatThrownBy(() -> item.keyKeywords().add("Kafka"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("핵심 키워드가 null이면 빈 목록으로 정규화한다")
        void nullKeyKeywords_DefaultsToEmptyList() {
            PersonalizationProfileLookupItem item = new PersonalizationProfileLookupItem(
                    new float[]{0.1f},
                    null
            );

            assertThat(item.keyKeywords()).isEmpty();
        }

        @Test
        @DisplayName("프로필 벡터가 null이면 null로 유지한다")
        void nullProfileVector_RemainsNull() {
            PersonalizationProfileLookupItem item = new PersonalizationProfileLookupItem(
                    null,
                    List.of("Spring")
            );

            assertThat(item.profileVector()).isNull();
        }
    }

    @Nested
    @DisplayName("profileVector")
    class ProfileVector {

        @Test
        @DisplayName("프로필 벡터 조회 결과를 변경해도 lookup item 내부 상태는 변경되지 않는다")
        void profileVectorAccessor_ReturnsCopy() {
            PersonalizationProfileLookupItem item = new PersonalizationProfileLookupItem(
                    new float[]{0.1f, 0.2f},
                    List.of("Spring")
            );

            float[] returnedVector = item.profileVector();
            returnedVector[0] = 9.9f;

            assertThat(item.profileVector()).containsExactly(0.1f, 0.2f);
        }
    }
}
