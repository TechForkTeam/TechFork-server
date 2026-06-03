package com.techfork.personalization.application;

import com.techfork.global.llm.EmbeddingClient;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizedProfileGeneratorTest {

    @Mock
    private UserActivityCollector userActivityCollector;

    @Mock
    private PersonalizationProfileAnalyzer personalizationProfileAnalyzer;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @InjectMocks
    private PersonalizedProfileGenerator personalizedProfileGenerator;

    @Test
    @DisplayName("사용자 활동 분석 결과를 임베딩해 개인화 프로필 문서로 저장한다")
    void generate_AnalyzesActivityDataEmbedsAndSavesProfileDocument() {
        Long userId = 1L;
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

        PersonalizationProfileDocument result = personalizedProfileGenerator.generate(userId);

        ArgumentCaptor<PersonalizationProfileDocument> documentCaptor = ArgumentCaptor.forClass(PersonalizationProfileDocument.class);
        verify(personalizationProfileDocumentRepository).save(documentCaptor.capture());

        PersonalizationProfileDocument savedDocument = documentCaptor.getValue();
        assertThat(result).isSameAs(savedDocument);
        assertThat(savedDocument.getUserId()).isEqualTo(userId);
        assertThat(savedDocument.getProfileText()).isEqualTo(analysis.profileText());
        assertThat(savedDocument.getProfileVector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(savedDocument.getInterests()).containsExactly("Java", "Spring", "Docker");
        assertThat(savedDocument.getKeyKeywords()).containsExactlyElementsOf(analysis.keyKeywords());

        verify(userActivityCollector).collect(userId);
        verify(personalizationProfileAnalyzer).analyze(activityData);
        verify(embeddingClient).embed(analysis.profileText());
    }

    private UserActivityData activityData(
            List<String> interests,
            List<PostActivityData> readPostData,
            List<PostActivityData> bookmarkedPostData,
            List<String> searchQueries
    ) {
        return new UserActivityData(interests, readPostData, bookmarkedPostData, searchQueries);
    }

    private PostActivityData postActivityData(String title, List<String> keywords, String readingEngagement) {
        return new PostActivityData(title, keywords, readingEngagement);
    }

    private PersonalizationProfileAnalysis profileAnalysis(String profileText, List<String> keyKeywords) {
        return new PersonalizationProfileAnalysis(profileText, keyKeywords);
    }
}
