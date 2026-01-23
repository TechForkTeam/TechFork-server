package com.techfork.domain.recommendation;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation_quality.ImprovedRecommendationTestCase;
import com.techfork.domain.recommendation_quality.RecommendationTestCase;
import com.techfork.domain.recommendation_quality.TrainTestSplit;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.entity.UserInterestKeyword;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.enums.EInterestKeyword;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserInterestCategoryRepository;
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

import java.time.LocalDateTime;
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
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final ReadPostRepository readPostRepository;
    private final UserProfileService userProfileService;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final EntityManager entityManager;

    /**
     * 테스트용 사용자 생성 및 읽은 글 이력 추가
     *
     * @param interestCategories 관심사 카테고리 목록
     * @param readPostCount      읽은 글 개수
     * @return 생성된 사용자
     */
    @Transactional
    public User createTestUser(List<EInterestCategory> interestCategories, int readPostCount) {
        // 사용자 생성
        User user = User.createSocialUser(
                SocialType.KAKAO,
                "testSocialId_" + UUID.randomUUID().toString(),
                "test_" + System.currentTimeMillis() + "@example.com",
                null
        );
        user = userRepository.save(user);

        log.info("테스트 사용자 생성: ID: {}", user.getId());

        // 관심사 카테고리 및 키워드 추가
        for (EInterestCategory category : interestCategories) {
            UserInterestCategory interestCategory = UserInterestCategory.create(user, category);
            userInterestCategoryRepository.save(interestCategory);

            // 해당 카테고리의 키워드 중 랜덤하게 2~4개 선택
            List<EInterestKeyword> availableKeywords = EInterestKeyword.getKeywordsByCategory(category);
            Collections.shuffle(availableKeywords);
            int keywordCount = 2 + (int) (Math.random() * 3); // 2~4개

            for (int i = 0; i < Math.min(keywordCount, availableKeywords.size()); i++) {
                UserInterestKeyword keyword = UserInterestKeyword.create(
                        interestCategory,
                        availableKeywords.get(i)
                );
                interestCategory.addKeyword(keyword);
            }

            userInterestCategoryRepository.save(interestCategory);
        }
        log.info("관심사 추가: {} (각 카테고리별 키워드 포함)", interestCategories);

        // 읽은 글 이력 생성 (관심사와 관련된 게시글 위주) - Batch Insert
        List<Post> posts = findPostsRelatedToInterests(interestCategories, readPostCount);

        LocalDateTime now = LocalDateTime.now();
        int batchSize = 20;
        for (int i = 0; i < Math.min(posts.size(), readPostCount); i++) {
            Post post = posts.get(i);
            ReadPost readPost = ReadPost.create(
                    user,
                    post,
                    now.minusDays(readPostCount - i),
                    180 // 3분 읽음
            );
            entityManager.persist(readPost);

            // Batch Insert를 위한 flush & clear
            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();

        log.info("읽은 글 이력 생성: {} 개 (Batch Insert)", Math.min(posts.size(), readPostCount));

        // UserProfile 생성 (임베딩 포함) - 동기 버전 사용
        try {
            userProfileService.generateUserProfileSync(user.getId());
            log.info("사용자 프로필 및 임베딩 생성 완료: userId={}", user.getId());
        } catch (Exception e) {
            log.error("사용자 프로필 생성 실패: userId={}", user.getId(), e);
            throw e;
        }

        return user;
    }

    /**
     * 관심사와 관련된 게시글 찾기 (회사명 또는 제목 기반)
     */
    @Transactional(readOnly = true)
    public List<Post> findPostsRelatedToInterests(List<EInterestCategory> interests, int limit) {
        // 관심사 카테고리별 키워드 매핑
        Map<EInterestCategory, List<String>> interestKeywords = new HashMap<>();
        interestKeywords.put(EInterestCategory.BACKEND, Arrays.asList("Spring", "Java", "Kotlin", "API", "서버", "Backend"));
        interestKeywords.put(EInterestCategory.AI_ML, Arrays.asList("AI", "ML", "머신러닝", "딥러닝", "LLM", "GPT"));
        interestKeywords.put(EInterestCategory.FRONTEND, Arrays.asList("React", "Vue", "JavaScript", "CSS", "UI", "Frontend"));
        interestKeywords.put(EInterestCategory.DATA_ENGINEERING, Arrays.asList("데이터", "분석", "Spark", "Kafka"));
        interestKeywords.put(EInterestCategory.DATA_SCIENCE, Arrays.asList("데이터", "분석", "ML", "통계"));
        interestKeywords.put(EInterestCategory.DATABASE, Arrays.asList("SQL", "Database", "MySQL", "PostgreSQL"));
        interestKeywords.put(EInterestCategory.DEVOPS, Arrays.asList("DevOps", "Docker", "Kubernetes", "CI/CD"));
        interestKeywords.put(EInterestCategory.CLOUD, Arrays.asList("AWS", "클라우드", "Cloud", "Azure"));

        List<Post> allPosts = postRepository.findAll(PageRequest.of(0, 500)).getContent();

        // 관심사 키워드와 매칭되는 게시글 찾기
        List<Post> relatedPosts = allPosts.stream()
                .filter(post -> {
                    String title = post.getTitle().toLowerCase();
                    return interests.stream()
                            .flatMap(interest -> interestKeywords.getOrDefault(interest, Collections.emptyList()).stream())
                            .anyMatch(keyword -> title.contains(keyword.toLowerCase()));
                })
                .limit(limit)
                .toList();

        // 매칭 안 되면 랜덤으로 선택
        if (relatedPosts.size() < limit && !allPosts.isEmpty()) {
            List<Post> remaining = new ArrayList<>(allPosts);
            remaining.removeAll(relatedPosts);
            Collections.shuffle(remaining);

            List<Post> combined = new ArrayList<>(relatedPosts);
            combined.addAll(remaining.subList(0, Math.min(limit - relatedPosts.size(), remaining.size())));
            return combined;
        }

        return relatedPosts;
    }

    /**
     * 테스트 케이스 생성
     * 사용자의 관심사와 읽은 글을 기반으로 관련도 점수 부여
     */
    @Transactional(readOnly = true)
    public RecommendationTestCase generateTestCase(User user, List<EInterestCategory> interests) {
        List<Long> readPostIds = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(user.getId(), PageRequest.of(0, 1000)).stream()
                .map(rp -> rp.getPost().getId())
                .toList();

        // 관련 있는 게시글 찾기 및 점수 부여
        List<Post> candidatePosts = findPostsRelatedToInterests(interests, 100);
        Map<Long, Integer> relevanceScores = new HashMap<>();

        for (Post post : candidatePosts) {
            // 이미 읽은 글은 제외
            if (readPostIds.contains(post.getId())) {
                continue;
            }

            // 관련도 점수 계산 (간단한 키워드 매칭 기반)
            int score = calculateRelevanceScore(post, interests);
            if (score > 0) {
                relevanceScores.put(post.getId(), score);
            }
        }

        List<String> interestNames = interests.stream()
                .map(EInterestCategory::getDisplayName)
                .toList();

        return RecommendationTestCase.builder()
                .userId(user.getId())
                .interests(interestNames)
                .readPostIds(readPostIds)
                .groundTruthScores(relevanceScores)
                .build();
    }


    /**
     * 프로필이 있는 기존 사용자 조회 (테스트용)
     *
     * @param count 조회할 사용자 수
     * @return 프로필이 있는 사용자 리스트
     * @throws IllegalStateException 프로필이 있는 사용자가 충분하지 않은 경우
     */
    @Transactional(readOnly = true)
    public List<User> getUsersWithProfile(int count) {
        // 1. 모든 사용자 중 프로필이 있는 사용자 찾기
        List<User> allUsers = userRepository.findAll();
        List<User> usersWithProfile = allUsers.stream()
                .filter(user -> userProfileDocumentRepository.findByUserId(user.getId()).isPresent())
                .filter(user -> user.getInterestCategories() != null && !user.getInterestCategories().isEmpty())
                .limit(count)
                .toList();

        // 2. 충분한 사용자가 있는지 확인
        if (usersWithProfile.size() < count) {
            throw new IllegalStateException(
                    String.format("프로필이 있는 사용자가 부족합니다. (현재 %d명, 필요 %d명). " +
                                    "먼저 createTestUser()로 테스트 사용자를 생성하세요.",
                            usersWithProfile.size(), count)
            );
        }

        log.info("프로필이 있는 기존 사용자 {} 명 조회 완료", usersWithProfile.size());
        List<Long> userIds = usersWithProfile.stream().map(User::getId).toList();
        return userRepository.findAllWithInterestCategoriesByIds(userIds);
    }

    /**
     * 게시글의 관련도 점수 계산 (1~5점)
     */
    private int calculateRelevanceScore(Post post, List<EInterestCategory> interests) {
        String title = post.getTitle().toLowerCase();

        // 관심사별 키워드 매칭 개수
        int matchCount = 0;
        for (EInterestCategory interest : interests) {
            if (title.contains(interest.getDisplayName().toLowerCase())) {
                matchCount++;
            }
        }

        // 매칭 개수에 따라 점수 부여
        if (matchCount >= 3) return 5;
        if (matchCount == 2) return 4;
        if (matchCount == 1) return 3;

        // 회사명이 유명 기업이면 기본 2점
        String company = post.getTechBlog().getCompanyName().toLowerCase();
        if (company.contains("네이버") || company.contains("kakao") ||
                company.contains("line") || company.contains("쿠팡")) {
            return 2;
        }

        return 1; // 기본 점수
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

        log.info("Train/Test Split 완료: Train={}, Test={}, 비율={:.2f}",
                trainIds.size(), testIds.size(), trainRatio);

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
                .findRecentReadPostsByUserIdWithMinDuration(user.getId(), PageRequest.of(0, 1000))
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
}
