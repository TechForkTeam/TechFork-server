package com.techfork.domain.recommendation.setup.components;

import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.dto.ImprovedRecommendationTestCase;
import com.techfork.domain.recommendation.dto.TrainTestSplit;
import com.techfork.domain.recommendation.dto.UserCreationResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.domain.user.service.UserProfileService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 추천 시스템 테스트를 위한 데이터 생성기
 * DB의 실제 게시글 데이터를 기반으로 테스트용 사용자 프로필과 Ground Truth 생성
 */
@Tag("evaluation-setup")
@Disabled("데이터 셋업용 - CI 제외")
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataGenerator {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReadPostRepository readPostRepository;
    private final UserProfileService userProfileService;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final EntityManager entityManager;

    // 분리된 컴포넌트
    private final PostMatcher postMatcher;
    private final UserTestDataBuilder userTestDataBuilder;
    private final GroundTruthGenerator groundTruthGenerator;

    // 공통 읽은 글 풀 (모든 사용자가 공통으로 읽는 글)
    private static List<Post> sharedReadPosts = null;

    /**
     * 공통 읽은 글 풀 초기화 (테스트 시작 전 호출)
     * 여러 사용자 생성 전에 한 번만 호출하면 모든 사용자가 같은 공통 글 풀 사용
     */
    public void resetSharedReadPosts() {
        sharedReadPosts = null;
        log.info("공통 읽은 글 풀 초기화됨");
    }

    /**
     * 테스트용 사용자 생성 - Leave-K-Out 방식
     *
     * @param interestCategories 관심사 카테고리 목록
     * @param readPostCount      읽은 글 개수 (프로필 구성용)
     * @param holdoutCount       숨길 정답 개수 (Ground Truth)
     * @return 사용자 생성 결과 (User + Ground Truth 게시글 ID)
     */
    @Transactional
    public UserCreationResult createTestUserWithGroundTruth(List<EInterestCategory> interestCategories, int readPostCount, int holdoutCount) {
        return createTestUserWithGroundTruth(interestCategories, readPostCount, holdoutCount, 30); // 기본 30% 공통
    }

    /**
     * 테스트용 사용자 생성 - Leave-K-Out 방식 (공통 읽은 글 비율 지정 가능)
     *
     * @param interestCategories    관심사 카테고리 목록
     * @param readPostCount         읽은 글 개수 (프로필 구성용)
     * @param holdoutCount          숨길 정답 개수 (Ground Truth)
     * @param sharedPostPercentage  공통 읽은 글 비율 (0~100, 기본 30)
     * @return 사용자 생성 결과 (User + Ground Truth 게시글 ID)
     */
    @Transactional
    public UserCreationResult createTestUserWithGroundTruth(
            List<EInterestCategory> interestCategories,
            int readPostCount,
            int holdoutCount,
            int sharedPostPercentage) {

        // 1. 사용자 생성 (관심사 포함)
        User user = userTestDataBuilder.createUserWithInterests(interestCategories);

        // 공통 읽은 글 풀 초기화 (첫 사용자 생성 시)
        int sharedPostCount = (int) (readPostCount * sharedPostPercentage / 100.0);
        int personalPostCount = readPostCount - sharedPostCount;

        if (sharedReadPosts == null) {
            // 전체 게시글 중 랜덤하게 공통 읽은 글 선택
            List<Post> allPosts = new ArrayList<>(postRepository.findAll());
            Collections.shuffle(allPosts);
            sharedReadPosts = allPosts.subList(0, Math.min(sharedPostCount, allPosts.size()));
            log.info("공통 읽은 글 풀 초기화: {} 개 (전체 {}개 중 {}%)",
                    sharedReadPosts.size(), readPostCount, sharedPostPercentage);
        }

        // 2. Leave-K-Out: 관심 있는 글 중 일부를 숨겨서 Ground Truth로 사용
        int totalRelatedPosts = personalPostCount + holdoutCount;
        List<Post> relatedPosts = postMatcher.findPostsRelatedToInterests(interestCategories, totalRelatedPosts);

        if (relatedPosts.size() < totalRelatedPosts) {
            log.warn("관심 있는 글이 부족합니다. 요청: {}, 실제: {}", totalRelatedPosts, relatedPosts.size());
        }

        // 순서 편향 제거: 랜덤 셔플
        // (관련도 높은 글이 앞쪽에 몰리는 것 방지)
        Collections.shuffle(relatedPosts);
        log.debug("관련 게시글 {} 개 셔플 완료", relatedPosts.size());

        // 읽은 글 = 공통 글 + 개인화 글
        List<Post> readPosts = new ArrayList<>();
        readPosts.addAll(sharedReadPosts);

        int actualPersonalCount = Math.min(relatedPosts.size(), personalPostCount);
        readPosts.addAll(relatedPosts.subList(0, actualPersonalCount));

        // 3. 읽은 글 저장 (프로필 구성용)
        userTestDataBuilder.createReadPosts(user, readPosts);
        log.info("읽은 글 구성: 공통 {} 개 + 개인화 {} 개 = 총 {} 개",
                sharedReadPosts.size(), actualPersonalCount, readPosts.size());

        // 4. 스크랩한 글 생성 (읽은 글 중 25% 정도를 스크랩)
        int scrapCount = Math.max(5, readPosts.size() / 4); // 최소 5개
        userTestDataBuilder.createScrapPosts(user, readPosts, scrapCount);
        log.info("스크랩한 글: {} 개 생성", scrapCount);

        // 5. 검색 기록 생성 (관심사 키워드 기반)
        List<String> searchKeywords = generateSearchKeywords(interestCategories);
        int searchHistoryCount = Math.min(30, searchKeywords.size() * 2); // 최대 30개
        userTestDataBuilder.createSearchHistories(user, searchKeywords, searchHistoryCount);
        log.info("검색 기록: {} 개 생성", searchHistoryCount);

        // UserProfile 생성 (임베딩 포함) - 동기 버전 사용
        // Ground Truth 점수 계산 전에 프로필 벡터가 필요함
        try {
            userProfileService.generateUserProfileSync(user.getId());
            log.info("사용자 프로필 및 임베딩 생성 완료: userId={}", user.getId());
        } catch (Exception e) {
            log.error("사용자 프로필 생성 실패: userId={}", user.getId(), e);
            throw e;
        }

        // 사용자 프로필 벡터 가져오기 (임베딩 기반 점수 계산용)
        float[] userProfileVector = null;
        Optional<UserProfileDocument> userProfileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        if (userProfileOpt.isPresent()) {
            userProfileVector = userProfileOpt.get().getProfileVector();
            log.debug("사용자 프로필 벡터 로드: {} 차원", userProfileVector != null ? userProfileVector.length : "null");
        } else {
            log.warn("사용자 프로필 벡터를 찾을 수 없습니다. 키워드 기반으로 fallback. userId={}", user.getId());
        }

        // 6. Ground Truth 관련도 점수 계산 (nDCG용) - 임베딩 유사도 기반
        int actualHoldoutCount = Math.min(relatedPosts.size() - actualPersonalCount, holdoutCount);
        List<Post> holdoutPosts = relatedPosts.subList(actualPersonalCount, actualPersonalCount + actualHoldoutCount);

        Map<Long, Integer> groundTruthScores = groundTruthGenerator.calculateGroundTruth(
                holdoutPosts,
                interestCategories,
                userProfileVector
        );

        log.info("Ground Truth 설정: {} 개 (평가용, 숨김)", actualHoldoutCount);

        // 점수 분포 로깅 및 검증
        Map<Integer, Long> scoreDistribution = groundTruthScores.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(score -> score, java.util.stream.Collectors.counting()));
        log.info("Ground Truth 점수 분포: {}", scoreDistribution);

        // Ground Truth 품질 검증
        groundTruthGenerator.validateGroundTruthQuality(groundTruthScores, interestCategories);

        return UserCreationResult.builder()
                .user(user)
                .groundTruthScores(groundTruthScores)
                .build();
    }


    /**
     * 읽은 글 이력을 Train/Test로 분할 (8:2 비율)
     *
     * @param readPostIds 전체 읽은 글 ID 목록
     * @param trainRatio  Train 세트 비율 (기본 0.8)
     * @return Train/Test 분할 결과
     */
    public TrainTestSplit splitReadHistory(List<Long> readPostIds, double trainRatio) {
        if (readPostIds == null || readPostIds.isEmpty()) {
            return TrainTestSplit.builder()
                    .trainPostIds(Collections.emptyList())
                    .testPostIds(Collections.emptyList())
                    .build();
        }

        // 시간순으로 정렬된 리스트를 Train/Test로 분할
        int totalSize = readPostIds.size();
        int trainSize = (int) (totalSize * trainRatio);

        List<Long> trainIds = readPostIds.subList(0, trainSize);
        List<Long> testIds = readPostIds.subList(trainSize, totalSize);

        log.info("Train/Test Split 완료: Train={}, Test={}, 비율={}",
                trainIds.size(), testIds.size(), String.format("%.2f", trainRatio));

        return TrainTestSplit.builder()
                .trainPostIds(new ArrayList<>(trainIds))
                .testPostIds(new ArrayList<>(testIds))
                .build();
    }

    /**
     * Train/Test Split 기반 개선된 테스트 케이스 생성
     *
     * @param user       평가 대상 사용자
     * @param interests  사용자 관심사
     * @param trainRatio Train 세트 비율 (기본 0.8)
     * @return Train/Test Split 기반 테스트 케이스
     */
    @Transactional(readOnly = true)
    public ImprovedRecommendationTestCase generateImprovedTestCase(
            User user,
            List<EInterestCategory> interests,
            double trainRatio) {

        // 읽은 글 이력 조회 (시간순)
        List<Long> readPostIds = readPostRepository
                .findRecentReadPostsByUserIdWithMinDuration(user.getId(), PageRequest.of(0, 10000))
                .stream()
                .map(rp -> rp.getPost().getId())
                .toList();

        // Train/Test Split
        TrainTestSplit split = splitReadHistory(readPostIds, trainRatio);

        List<String> interestNames = interests.stream()
                .map(EInterestCategory::getDisplayName)
                .toList();

        log.info("===== 개선된 테스트 케이스 생성 =====");
        log.info("사용자 ID: {}", user.getId());
        log.info("관심사: {}", interestNames);
        log.info("전체 읽은 글: {} 개", readPostIds.size());
        log.info("Train Set: {} 개", split.getTrainSize());
        log.info("Test Set: {} 개", split.getTestSize());
        log.info("Test Set ID 샘플: {}", split.getTestPostIds().stream().limit(5).toList());

        return ImprovedRecommendationTestCase.builder()
                .userId(user.getId())
                .interests(interestNames)
                .trainTestSplit(split)
                .build();
    }

    /**
     * Train/Test Split 기반 개선된 테스트 케이스 생성 (기본 비율 0.8)
     */
    @Transactional(readOnly = true)
    public ImprovedRecommendationTestCase generateImprovedTestCase(
            User user,
            List<EInterestCategory> interests) {
        return generateImprovedTestCase(user, interests, 0.8);
    }

    /**
     * 관심사 기반 검색 키워드 생성
     */
    private List<String> generateSearchKeywords(List<EInterestCategory> interests) {
        Map<EInterestCategory, List<String>> keywordMap = new HashMap<>();

        // BACKEND 관련 검색어
        keywordMap.put(EInterestCategory.BACKEND, Arrays.asList(
                "Spring Boot", "Java", "Kotlin", "API 설계", "서버 아키텍처",
                "마이크로서비스", "REST API", "데이터베이스 최적화", "JPA", "Hibernate"
        ));

        // FRONTEND 관련 검색어
        keywordMap.put(EInterestCategory.FRONTEND, Arrays.asList(
                "React", "Vue.js", "JavaScript", "TypeScript", "CSS",
                "UI/UX", "웹 성능 최적화", "반응형 디자인", "Next.js", "Webpack"
        ));

        // AI/ML 관련 검색어
        keywordMap.put(EInterestCategory.AI_ML, Arrays.asList(
                "딥러닝", "머신러닝", "LLM", "ChatGPT", "AI 모델",
                "TensorFlow", "PyTorch", "자연어처리", "컴퓨터 비전", "데이터 분석"
        ));

        // DATABASE 관련 검색어
        keywordMap.put(EInterestCategory.DATABASE, Arrays.asList(
                "MySQL", "PostgreSQL", "Redis", "MongoDB", "쿼리 최적화",
                "인덱싱", "데이터베이스 설계", "SQL", "NoSQL", "데이터 모델링"
        ));

        // DATA_SCIENCE 관련 검색어
        keywordMap.put(EInterestCategory.DATA_SCIENCE, Arrays.asList(
                "데이터 분석", "통계", "Python", "데이터 시각화", "Pandas",
                "빅데이터", "데이터 파이프라인", "ETL", "데이터 엔지니어링", "Jupyter"
        ));

        // DEVOPS 관련 검색어
        keywordMap.put(EInterestCategory.DEVOPS, Arrays.asList(
                "Docker", "Kubernetes", "CI/CD", "AWS", "클라우드",
                "인프라", "모니터링", "로깅", "배포 자동화", "Terraform"
        ));

        // MOBILE 관련 검색어
        keywordMap.put(EInterestCategory.ANDROID, Arrays.asList(
                "Android", "React Native", "Flutter", "모바일 앱",
                "Kotlin", "크로스플랫폼", "앱 성능", "모바일 UI"
        ));

        // SECURITY 관련 검색어
        keywordMap.put(EInterestCategory.SECURITY, Arrays.asList(
                "보안", "인증", "암호화", "OAuth", "JWT",
                "해킹 방어", "보안 취약점", "HTTPS", "방화벽", "보안 아키텍처"
        ));

        // 사용자의 관심사에 해당하는 키워드들을 모두 모음
        List<String> allKeywords = new ArrayList<>();
        for (EInterestCategory interest : interests) {
            List<String> keywords = keywordMap.getOrDefault(interest, Collections.emptyList());
            allKeywords.addAll(keywords);
        }

        // 키워드가 없으면 기본 키워드 사용
        if (allKeywords.isEmpty()) {
            allKeywords.addAll(Arrays.asList(
                    "개발", "프로그래밍", "코딩", "소프트웨어", "기술 블로그"
            ));
        }

        return allKeywords;
    }
}
