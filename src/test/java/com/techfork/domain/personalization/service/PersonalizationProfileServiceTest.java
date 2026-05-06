package com.techfork.domain.personalization.service;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readhistory.entity.SearchHistory;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.activity.readhistory.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.domain.personalization.document.PersonalizationProfileDocument;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.entity.UserInterestCategory;
import com.techfork.domain.useraccount.entity.UserInterestKeyword;
import com.techfork.domain.useraccount.enums.EInterestCategory;
import com.techfork.domain.useraccount.enums.EInterestKeyword;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserInterestCategoryRepository;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileServiceTest {

    @Mock
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

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
        List<ReadPost> readPosts = List.of(
                readPost("30초 포스트", List.of("Java"), 20),
                readPost("90초 포스트", List.of("Spring"), 60),
                readPost("300초 포스트", List.of("JPA"), 200),
                readPost("600초 포스트", List.of("Kafka"), 400),
                readPost("601초 포스트", List.of("Docker"), 700),
                readPost("null 포스트", List.of("Elastic"), null)
        );
        List<Bookmark> bookmarks = List.of(
                bookmark("북마크 포스트", List.of("Kubernetes", "Helm"))
        );

        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId))
                .willReturn(List.of(
                        interestCategory(user, EInterestCategory.BACKEND, EInterestKeyword.JAVA, EInterestKeyword.SPRING),
                        interestCategory(user, EInterestCategory.DEVOPS, EInterestKeyword.DOCKER)
                ));
        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(anyLong(), any()))
                .willReturn(readPosts);
        given(bookmarkRepository.findRecentBookmarksByUserId(anyLong(), any()))
                .willReturn(bookmarks);
        given(searchHistoryRepository.findRecentSearchHistoriesByUserId(anyLong(), any()))
                .willReturn(List.of(
                        SearchHistory.create(user, "Spring Batch", LocalDateTime.now()),
                        SearchHistory.create(user, "Elasticsearch vector", LocalDateTime.now())
                ));
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

        verify(userRepository).findById(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    @Test
    @DisplayName("LLM 응답을 파싱하지 못하면 전체 텍스트를 프로필로 fallback 저장한다")
    void generatePersonalizationProfileSync_FallsBackToFullTextWhenSectionsAreMissing() {
        Long userId = 2L;
        User user = createUser(userId);
        String llmResponse = "섹션 없이도 전체 응답을 개인화 프로필로 저장해야 한다";

        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId)).willReturn(List.of());
        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(anyLong(), any())).willReturn(List.of());
        given(bookmarkRepository.findRecentBookmarksByUserId(anyLong(), any())).willReturn(List.of());
        given(searchHistoryRepository.findRecentSearchHistoriesByUserId(anyLong(), any())).willReturn(List.of());
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
    }

    @Test
    @DisplayName("추천 생성이 실패해도 개인화 프로필 저장은 유지된다")
    void generatePersonalizationProfileSync_RecommendationFailureDoesNotBreakProfileSave() {
        Long userId = 3L;
        User user = createUser(userId);

        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId)).willReturn(List.of());
        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(anyLong(), any())).willReturn(List.of());
        given(bookmarkRepository.findRecentBookmarksByUserId(anyLong(), any())).willReturn(List.of());
        given(searchHistoryRepository.findRecentSearchHistoriesByUserId(anyLong(), any())).willReturn(List.of());
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
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    private User createUser(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "social-" + userId, "user" + userId + "@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private UserInterestCategory interestCategory(User user, EInterestCategory category, EInterestKeyword... keywords) {
        UserInterestCategory userInterestCategory = UserInterestCategory.create(user, category);
        for (EInterestKeyword keyword : keywords) {
            userInterestCategory.addKeyword(UserInterestKeyword.create(userInterestCategory, keyword));
        }
        return userInterestCategory;
    }

    private ReadPost readPost(String title, List<String> keywords, Integer durationSeconds) {
        ReadPost readPost = org.mockito.Mockito.mock(ReadPost.class);
        Post post = mockPost(title, keywords);

        given(readPost.getPost()).willReturn(post);
        given(readPost.getReadDurationSeconds()).willReturn(durationSeconds);

        return readPost;
    }

    private Bookmark bookmark(String title, List<String> keywords) {
        Bookmark bookmark = org.mockito.Mockito.mock(Bookmark.class);
        Post post = mockPost(title, keywords);

        given(bookmark.getPost()).willReturn(post);

        return bookmark;
    }

    private Post mockPost(String title, List<String> keywords) {
        Post post = org.mockito.Mockito.mock(Post.class);
        List<PostKeyword> postKeywords = keywords.stream()
                .map(this::mockPostKeyword)
                .toList();

        given(post.getTitle()).willReturn(title);
        given(post.getKeywords()).willReturn(postKeywords);

        return post;
    }

    private PostKeyword mockPostKeyword(String keyword) {
        PostKeyword postKeyword = org.mockito.Mockito.mock(PostKeyword.class);
        given(postKeyword.getKeyword()).willReturn(keyword);
        return postKeyword;
    }
}
