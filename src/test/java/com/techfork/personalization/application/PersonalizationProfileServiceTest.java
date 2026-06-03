package com.techfork.personalization.application;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileServiceTest {

    @Mock
    private UserActivityCollector userActivityCollector;

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private LlmClient llmClient;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private PersonalizationProfileService personalizationProfileService;

    @Test
    @DisplayName("사용자 활동 데이터를 모아 개인화 프로필을 생성하고 저장한다")
    void generatePersonalizationProfileSync_CollectsActivityDataParsesAndSavesProfile() {
        Long userId = 1L;
        User user = createUser(userId);
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

        given(userActivityCollector.collect(userId)).willReturn(activityData);
        given(llmClient.call(anyString(), anyString()))
                .willReturn("""
                        ### PROFILE
                        Java와 Spring 기반 백엔드, Docker 중심 운영 자동화, Elasticsearch 검색 최적화에 집중하는 사용자

                        ### KEYWORDS
                        Java, Spring, Docker, Elasticsearch, Batch
                        """);
        given(embeddingClient.embed(anyString())).willReturn(List.of(0.1f, 0.2f, 0.3f));
        given(personalizationProfileDocumentRepository.save(any(PersonalizationProfileDocument.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendationService.generateRecommendationsForUser(user)).willReturn(5);

        personalizationProfileService.generatePersonalizationProfileSync(userId);

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

        ArgumentCaptor<PersonalizationProfileDocument> documentCaptor = ArgumentCaptor.forClass(PersonalizationProfileDocument.class);
        verify(personalizationProfileDocumentRepository).save(documentCaptor.capture());

        PersonalizationProfileDocument savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getUserId()).isEqualTo(userId);
        assertThat(savedDocument.getProfileText())
                .isEqualTo("Java와 Spring 기반 백엔드, Docker 중심 운영 자동화, Elasticsearch 검색 최적화에 집중하는 사용자");
        assertThat(savedDocument.getProfileVector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(savedDocument.getInterests()).containsExactly("Java", "Spring", "Docker");
        assertThat(savedDocument.getKeyKeywords())
                .containsExactly("Java", "Spring", "Docker", "Elasticsearch", "Batch");

        verify(userActivityCollector).collect(userId);
        verify(userRepository).findById(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    @Test
    @DisplayName("LLM 응답을 파싱하지 못하면 전체 텍스트를 프로필로 fallback 저장한다")
    void generatePersonalizationProfileSync_FallsBackToFullTextWhenSectionsAreMissing() {
        Long userId = 2L;
        User user = createUser(userId);
        String llmResponse = "섹션 없이도 전체 응답을 개인화 프로필로 저장해야 한다";

        given(userActivityCollector.collect(userId)).willReturn(emptyActivityData());
        given(llmClient.call(anyString(), anyString())).willReturn(llmResponse);
        given(embeddingClient.embed(llmResponse)).willReturn(List.of(1.0f, 2.0f));
        given(personalizationProfileDocumentRepository.save(any(PersonalizationProfileDocument.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        personalizationProfileService.generatePersonalizationProfileSync(userId);

        ArgumentCaptor<PersonalizationProfileDocument> documentCaptor = ArgumentCaptor.forClass(PersonalizationProfileDocument.class);
        verify(personalizationProfileDocumentRepository).save(documentCaptor.capture());

        PersonalizationProfileDocument savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getProfileText()).isEqualTo(llmResponse);
        assertThat(savedDocument.getKeyKeywords()).isEmpty();
        assertThat(savedDocument.getProfileVector()).containsExactly(1.0f, 2.0f);
        verify(userActivityCollector).collect(userId);
    }

    @Test
    @DisplayName("LLM 응답의 핵심 키워드는 최대 5개까지만 저장한다")
    void generatePersonalizationProfileSync_LimitsKeyKeywordsToFive() {
        Long userId = 4L;
        User user = createUser(userId);

        given(userActivityCollector.collect(userId)).willReturn(emptyActivityData());
        given(llmClient.call(anyString(), anyString()))
                .willReturn("""
                        ### PROFILE
                        Java와 Spring 기반 백엔드 성능 최적화에 집중하는 사용자

                        ### KEYWORDS
                        Java, Spring, JPA, Redis, Kafka, Kubernetes, Elasticsearch
                        """);
        given(embeddingClient.embed(anyString())).willReturn(List.of(0.4f, 0.5f));
        given(personalizationProfileDocumentRepository.save(any(PersonalizationProfileDocument.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        personalizationProfileService.generatePersonalizationProfileSync(userId);

        ArgumentCaptor<PersonalizationProfileDocument> documentCaptor = ArgumentCaptor.forClass(PersonalizationProfileDocument.class);
        verify(personalizationProfileDocumentRepository).save(documentCaptor.capture());

        assertThat(documentCaptor.getValue().getKeyKeywords())
                .containsExactly("Java", "Spring", "JPA", "Redis", "Kafka");
        verify(userActivityCollector).collect(userId);
    }

    @Test
    @DisplayName("추천 생성이 실패해도 개인화 프로필 저장은 유지된다")
    void generatePersonalizationProfileSync_RecommendationFailureDoesNotBreakProfileSave() {
        Long userId = 3L;
        User user = createUser(userId);

        given(userActivityCollector.collect(userId)).willReturn(emptyActivityData());
        given(llmClient.call(anyString(), anyString()))
                .willReturn("""
                        ### PROFILE
                        추천 실패와 무관하게 저장되어야 하는 프로필

                        ### KEYWORDS
                        테스트, 회귀
                        """);
        given(embeddingClient.embed(anyString())).willReturn(List.of(9.0f, 8.0f));
        given(personalizationProfileDocumentRepository.save(any(PersonalizationProfileDocument.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendationService.generateRecommendationsForUser(user))
                .willThrow(new RuntimeException("recommendation failure"));

        assertThatCode(() -> personalizationProfileService.generatePersonalizationProfileSync(userId))
                .doesNotThrowAnyException();

        verify(personalizationProfileDocumentRepository).save(any(PersonalizationProfileDocument.class));
        verify(userActivityCollector).collect(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
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

    private User createUser(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "social-" + userId, "user" + userId + "@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
