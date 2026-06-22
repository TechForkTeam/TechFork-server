package com.techfork.personalization.application.generation;

import com.techfork.personalization.application.activity.PostActivityData;
import com.techfork.personalization.application.activity.UserActivityData;
import com.techfork.global.llm.LlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileAnalyzerTest {

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private PersonalizationProfileAnalyzer personalizationProfileAnalyzer;

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("사용자 활동 데이터를 프롬프트에 담아 LLM 프로필과 키워드로 분석한다")
        void activityDataProvided_BuildsPromptAndParsesProfileAndKeywords() {
            UserActivityData activityData = activityData(
                    List.of("Java", "Spring", "Docker"),
                    List.of(
                            postActivityData("30초 포스트", List.of("Java"), "가볍게 훑어봄"),
                            postActivityData("90초 포스트", List.of("Spring"), "빠르게 읽음"),
                            postActivityData("300초 포스트", List.of("JPA"), "읽음"),
                            postActivityData("600초 포스트", List.of("Kafka"), "정독함"),
                            postActivityData("601초 포스트", List.of("Docker"), "깊게 읽음"),
                            postActivityData("null 포스트", List.of("Elastic"), "읽음")
                    ),
                    List.of(postActivityData("북마크 포스트", List.of("Kubernetes", "Helm"), null)),
                    List.of("Spring Batch", "Elasticsearch vector")
            );
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            ### PROFILE
                            Java와 Spring 기반 백엔드, Docker 중심 운영 자동화, Elasticsearch 검색 최적화에 집중하는 사용자

                            ### KEYWORDS
                            Java, Spring, Docker, Elasticsearch, Batch
                            """);

            PersonalizationProfileAnalysis result = personalizationProfileAnalyzer.analyze(activityData);

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            verify(llmClient).call(anyString(), promptCaptor.capture());

            String prompt = promptCaptor.getValue();
            assertThat(prompt)
                    .contains("Java")
                    .contains("Spring")
                    .contains("Docker")
                    .contains("30초 포스트")
                    .contains("90초 포스트")
                    .contains("300초 포스트")
                    .contains("600초 포스트")
                    .contains("601초 포스트")
                    .contains("null 포스트")
                    .contains("북마크 포스트")
                    .contains("Spring Batch")
                    .contains("Elasticsearch vector")
                    .contains("가볍게 훑어봄")
                    .contains("빠르게 읽음")
                    .contains("읽음")
                    .contains("정독함")
                    .contains("깊게 읽음");
            assertThat(result.profileText())
                    .isEqualTo("Java와 Spring 기반 백엔드, Docker 중심 운영 자동화, Elasticsearch 검색 최적화에 집중하는 사용자");
            assertThat(result.keyKeywords())
                    .containsExactly("Java", "Spring", "Docker", "Elasticsearch", "Batch");
        }

        @Test
        @DisplayName("LLM 응답을 파싱하지 못하면 전체 텍스트를 프로필로 fallback한다")
        void sectionsMissing_FallsBackToFullText() {
            String llmResponse = "섹션 없이도 전체 응답을 개인화 프로필로 저장해야 한다";
            given(llmClient.call(anyString(), anyString())).willReturn(llmResponse);

            PersonalizationProfileAnalysis result = personalizationProfileAnalyzer.analyze(emptyActivityData());

            assertThat(result.profileText()).isEqualTo(llmResponse);
            assertThat(result.keyKeywords()).isEmpty();
        }

        @Test
        @DisplayName("LLM 응답의 핵심 키워드는 최대 5개까지만 반환한다")
        void manyKeywords_LimitsKeyKeywordsToFive() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            ### PROFILE
                            Java와 Spring 기반 백엔드 성능 최적화에 집중하는 사용자

                            ### KEYWORDS
                            Java, Spring, JPA, Redis, Kafka, Kubernetes, Elasticsearch
                            """);

            PersonalizationProfileAnalysis result = personalizationProfileAnalyzer.analyze(emptyActivityData());

            assertThat(result.keyKeywords())
                    .containsExactly("Java", "Spring", "JPA", "Redis", "Kafka");
        }
    }

    private UserActivityData activityData(
            List<String> interests,
            List<PostActivityData> readPostData,
            List<PostActivityData> bookmarkedPostData,
            List<String> searchQueries
    ) {
        return new UserActivityData(interests, readPostData, bookmarkedPostData, searchQueries);
    }

    private UserActivityData emptyActivityData() {
        return activityData(List.of(), List.of(), List.of(), List.of());
    }

    private PostActivityData postActivityData(String title, List<String> keywords, String readingEngagement) {
        return new PostActivityData(title, keywords, readingEngagement);
    }
}
