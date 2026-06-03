package com.techfork.personalization.application;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.global.llm.EmbeddingClient;
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
    private PersonalizationProfileAnalyzer personalizationProfileAnalyzer;

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private PersonalizationProfileService personalizationProfileService;

    @Test
    @DisplayName("사용자 활동 데이터를 분석해 개인화 프로필을 저장하고 추천을 생성한다")
    void generatePersonalizationProfileSync_AnalyzesActivityDataSavesProfileAndGeneratesRecommendations() {
        Long userId = 1L;
        User user = createUser(userId);
        UserActivityData activityData = activityData(
                List.of("Java", "Spring", "Docker"),
                List.of(postActivityData("읽은 포스트", List.of("Java"), "정독함")),
                List.of(postActivityData("북마크 포스트", List.of("Kubernetes", "Helm"), null)),
                List.of("Spring Batch", "Elasticsearch vector")
        );
        PersonalizationProfileAnalysis analysis = profileAnalysis(
                "Java와 Spring 기반 백엔드, Docker 중심 운영 자동화, Elasticsearch 검색 최적화에 집중하는 사용자",
                List.of("Java", "Spring", "Docker", "Elasticsearch", "Batch")
        );

        given(userActivityCollector.collect(userId)).willReturn(activityData);
        given(personalizationProfileAnalyzer.analyze(activityData)).willReturn(analysis);
        given(embeddingClient.embed(analysis.profileText())).willReturn(List.of(0.1f, 0.2f, 0.3f));
        given(personalizationProfileDocumentRepository.save(any(PersonalizationProfileDocument.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendationService.generateRecommendationsForUser(user)).willReturn(5);

        personalizationProfileService.generatePersonalizationProfileSync(userId);

        ArgumentCaptor<PersonalizationProfileDocument> documentCaptor = ArgumentCaptor.forClass(PersonalizationProfileDocument.class);
        verify(personalizationProfileDocumentRepository).save(documentCaptor.capture());

        PersonalizationProfileDocument savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getUserId()).isEqualTo(userId);
        assertThat(savedDocument.getProfileText()).isEqualTo(analysis.profileText());
        assertThat(savedDocument.getProfileVector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(savedDocument.getInterests()).containsExactly("Java", "Spring", "Docker");
        assertThat(savedDocument.getKeyKeywords()).containsExactlyElementsOf(analysis.keyKeywords());

        verify(userActivityCollector).collect(userId);
        verify(personalizationProfileAnalyzer).analyze(activityData);
        verify(embeddingClient).embed(analysis.profileText());
        verify(userRepository).findById(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    @Test
    @DisplayName("추천 생성이 실패해도 개인화 프로필 저장은 유지된다")
    void generatePersonalizationProfileSync_RecommendationFailureDoesNotBreakProfileSave() {
        Long userId = 3L;
        User user = createUser(userId);
        UserActivityData activityData = emptyActivityData();
        PersonalizationProfileAnalysis analysis = profileAnalysis(
                "추천 실패와 무관하게 저장되어야 하는 프로필",
                List.of("테스트", "회귀")
        );

        given(userActivityCollector.collect(userId)).willReturn(activityData);
        given(personalizationProfileAnalyzer.analyze(activityData)).willReturn(analysis);
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
        verify(personalizationProfileAnalyzer).analyze(activityData);
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

    private PersonalizationProfileAnalysis profileAnalysis(String profileText, List<String> keyKeywords) {
        return new PersonalizationProfileAnalysis(profileText, keyKeywords);
    }

    private User createUser(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "social-" + userId, "user" + userId + "@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
