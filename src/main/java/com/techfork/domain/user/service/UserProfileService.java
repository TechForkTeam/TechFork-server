package com.techfork.domain.user.service;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserInterestCategoryRepository;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final ReadPostRepository readPostRepository;
    private final ScrabPostRepository scrabPostRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final LlmClient llmClient;
    private final EmbeddingClient embeddingClient;

    @Async
    @Transactional
    public void generateUserProfile(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

            // 1. 사용자 데이터 수집
            UserActivityData activityData = collectUserActivityData(user);

            // 2. LLM으로 프로필 텍스트 생성
            String profileText = generateProfileTextWithLLM(activityData);

            // 3. 임베딩 벡터 생성
            float[] profileVector = generateEmbeddingVector(profileText);

            // 4. Elasticsearch에 저장
            UserProfileDocument profileDocument = UserProfileDocument.create(
                    userId,
                    profileText,
                    profileVector,
                    activityData.interests,
                    activityData.preferredTopics
            );

            userProfileDocumentRepository.save(profileDocument);

            log.info("User profile generated successfully for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to generate user profile for userId: {}", userId, e);
        }
    }

    private UserActivityData collectUserActivityData(User user) {
        // 관심사
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserWithKeywords(user);
        List<String> interests = categories.stream()
                .flatMap(c -> c.getKeywords().stream())
                .map(k -> k.getKeyword().getDisplayName())
                .toList();

        // 최근 읽은 포스트 (최대 20개)
        List<ReadPost> readPosts = readPostRepository.findRecentReadPostsByUser(user, PageRequest.of(0, 20));
        List<String> readPostTitles = readPosts.stream()
                .map(rp -> rp.getPost().getTitle())
                .toList();

        // 스크랩한 포스트 (최대 20개)
        List<ScrabPost> scrapPosts = scrabPostRepository.findRecentScrapPostsByUser(user, PageRequest.of(0, 20));
        List<String> scrapPostTitles = scrapPosts.stream()
                .map(sp -> sp.getPost().getTitle())
                .toList();

        // 검색 기록 (최대 30개)
        List<SearchHistory> searchHistories = searchHistoryRepository.findRecentSearchHistoriesByUser(user, PageRequest.of(0, 30));
        List<String> searchWords = searchHistories.stream()
                .map(SearchHistory::getSearchWord)
                .toList();

        // 선호 주제 (읽은 포스트의 회사명 집계)
        List<String> preferredTopics = readPosts.stream()
                .map(rp -> rp.getPost().getCompany())
                .distinct()
                .toList();

        return new UserActivityData(interests, readPostTitles, scrapPostTitles, searchWords, preferredTopics);
    }

    private String generateProfileTextWithLLM(UserActivityData data) {
        String systemPrompt = "당신은 테크 블로그 플랫폼의 사용자 프로필 분석 전문가입니다. 사용자의 활동 데이터를 분석하여 검색 고도화와 포스트 추천에 최적화된 프로필을 생성합니다.";
        String userPrompt = buildProfileGenerationPrompt(data);
        return llmClient.call(systemPrompt, userPrompt);
    }

    private String buildProfileGenerationPrompt(UserActivityData data) {
        return String.format("""
                아래 사용자의 활동 데이터를 분석하여 검색 고도화와 포스트 추천에 최적화된 프로필을 생성해주세요.

                ## 사용자 데이터

                ### 관심 기술 스택 및 분야
                %s

                ### 최근 읽은 포스트 제목
                %s

                ### 스크랩한 포스트 제목
                %s

                ### 검색 기록
                %s

                ## 요구사항

                다음 형식으로 구조화된 프로필을 생성해주세요:

                1. **기술적 관심사 요약** (2-3문장)
                   - 사용자가 주로 관심을 갖는 기술 스택, 프레임워크, 도구
                   - 선호하는 개발 분야 (백엔드, 프론트엔드, AI, 인프라 등)

                2. **콘텐츠 선호 패턴** (2-3문장)
                   - 어떤 유형의 콘텐츠를 선호하는지 (튜토리얼, 아키텍처, 트러블슈팅, 신기술 소개 등)
                   - 어떤 회사나 팀의 기술 블로그를 자주 읽는지

                3. **검색 의도 분석** (2-3문장)
                   - 검색 기록에서 드러나는 학습 목적이나 해결하려는 문제
                   - 반복되는 검색 주제나 패턴

                4. **추천 키워드** (쉼표로 구분된 15-20개의 키워드)
                   - 검색 쿼리 확장에 사용할 관련 기술 용어
                   - 유사한 관심사를 가진 사용자가 찾을 만한 키워드
                   - 영문과 한글 키워드 모두 포함

                5. **프로필 요약** (1-2문장, 벡터 임베딩 최적화용)
                   - 사용자의 기술적 페르소나를 한 줄로 압축
                   - 추천 시스템이 유사 사용자를 찾는데 활용할 핵심 설명

                데이터가 부족한 경우 관심 기술 스택을 기반으로 일반적인 프로필을 생성해주세요.
                """,
                formatList(data.interests),
                formatList(data.readPostTitles),
                formatList(data.scrapPostTitles),
                formatList(data.searchWords)
        );
    }

    private String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- (데이터 없음)";
        }
        return items.stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
    }

    private float[] generateEmbeddingVector(String profileText) {
        // OpenAI text-embedding-3-large (3072 dimensions)
        List<Float> embedding = embeddingClient.embed(profileText);

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }
        return vector;
    }

    private record UserActivityData(
            List<String> interests,
            List<String> readPostTitles,
            List<String> scrapPostTitles,
            List<String> searchWords,
            List<String> preferredTopics
    ) {}
}
