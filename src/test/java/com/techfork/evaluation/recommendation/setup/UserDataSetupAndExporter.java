package com.techfork.evaluation.recommendation.setup;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.evaluation.recommendation.setup.components.FileExporter;
import com.techfork.evaluation.recommendation.setup.components.TestDataGenerator;
import com.techfork.evaluation.recommendation.setup.components.TestDataGenerator.UserCreationResult;
import com.techfork.evaluation.recommendation.util.EvaluationFixtureLoader;
import com.techfork.domain.personalization.document.PersonalizationProfileDocument;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Testcontainer에서 사용자 데이터 생성 및 JSON으로 export
 * <p>
 * 실행 방법:
 * ./gradlew test --tests UserDataSetupAndExporter
 * <p>
 * 주의: 게시글 데이터가 먼저 로드되어 있어야 합니다 (PostDataExporter 먼저 실행 필요)
 */
@Tag("evaluation-setup")
@Disabled("수동 실행용 - CI 제외")
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDataSetupAndExporter extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReadPostRepository readPostRepository;

    @Autowired
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Autowired
    private TestDataGenerator testDataGenerator;

    @Autowired
    private EvaluationFixtureLoader fixtureLoader;

    @Autowired
    private FileExporter fileExporter;

    private static final int USER_COUNT = 15;
    private static final int READ_POST_COUNT = 80;   // 프로필 구성용 (읽은 글) - 1100개 데이터셋 기준 (약 7%)
    private static final int HOLDOUT_COUNT = 30;     // Ground Truth (평가용, 숨김) - 평가 샘플 (약 2.7%)

    // 실제 DB ID (Local) -> 원격 DB ID (Remote) 매핑용
    private static Map<Long, Long> actualToRemotePostIdMap = new HashMap<>();

    @Test
    @Order(1)
    @DisplayName("STEP 1: 게시글 픽스처 로드 (posts.json, post-documents.json)")
    void step1_LoadPostFixtures() {
        log.info("===== STEP 1: 게시글 픽스처 로드 =====");
        log.info("주의: PostDataExporter를 먼저 실행하여 게시글 데이터를 export해야 합니다.");

        try {
            Map<Long, Post> remoteToActualMap = fixtureLoader.loadPostsOnly();

            // 역매핑 맵 생성 (실제 DB ID -> 원래 원격 ID)
            actualToRemotePostIdMap.clear();
            remoteToActualMap.forEach((remoteId, post) ->
                actualToRemotePostIdMap.put(post.getId(), remoteId));

            log.info("✓ 게시글 픽스처 로드 및 ID 매핑 완료 ({} 개)", actualToRemotePostIdMap.size());
        } catch (Exception e) {
            log.error("게시글 픽스처 로드 실패. PostDataExporter를 먼저 실행하세요.", e);
            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName("STEP 2: 테스트 사용자 15명 생성 (임베딩 포함)")
    @Transactional
    @Commit
    void step2_CreateTestUsers() throws IOException {
        log.info("===== STEP 2: 테스트 사용자 생성 =====");

        List<List<EInterestCategory>> interestCombos = Arrays.asList(
                // Backend 중심 (4명)
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.DATABASE),
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.ARCHITECTURE),
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.SECURITY),
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.FRONTEND),

                // Frontend 중심 (3명)
                Arrays.asList(EInterestCategory.FRONTEND, EInterestCategory.PRODUCT_UX),
                Arrays.asList(EInterestCategory.FRONTEND, EInterestCategory.ARCHITECTURE),
                Arrays.asList(EInterestCategory.FRONTEND),

                // Data & AI (3명)
                Arrays.asList(EInterestCategory.AI_ML, EInterestCategory.DATA_SCIENCE),
                Arrays.asList(EInterestCategory.DATA_ENGINEERING, EInterestCategory.DATABASE),
                Arrays.asList(EInterestCategory.AI_ML, EInterestCategory.CLOUD),

                // DevOps & Infrastructure (3명)
                Arrays.asList(EInterestCategory.DEVOPS, EInterestCategory.CLOUD),
                Arrays.asList(EInterestCategory.CLOUD, EInterestCategory.ARCHITECTURE),
                Arrays.asList(EInterestCategory.SYSTEMS_OS, EInterestCategory.NETWORKING),

                // Mobile (2명)
                Arrays.asList(EInterestCategory.IOS, EInterestCategory.ANDROID),
                Arrays.asList(EInterestCategory.IOS, EInterestCategory.PRODUCT_UX)
        );

        Map<Long, Map<Long, Integer>> userGroundTruthMap = new HashMap<>();

        for (int i = 0; i < USER_COUNT; i++) {
            List<EInterestCategory> interests = interestCombos.get(i);

            log.info("사용자 {}/{} 생성 중... (관심사: {})", i + 1, USER_COUNT, interests);

            UserCreationResult result = testDataGenerator.createTestUserWithGroundTruth(interests, READ_POST_COUNT, HOLDOUT_COUNT);
            User user = result.user();

            userGroundTruthMap.put(user.getId(), result.groundTruthScores());

            log.info("✓ 사용자 생성 완료: ID={}, 관심사={}, 읽은 글={} 개, Ground Truth={} 개",
                    user.getId(), interests, READ_POST_COUNT, result.groundTruthScores().size());
        }

        log.info("===== STEP 2 완료: {} 명 사용자 생성 완료 =====\n", USER_COUNT);

        List<User> users = userRepository.findAll();
        log.info("총 생성된 사용자: {} 명", users.size());

        long profileCount = users.stream()
                .filter(u -> personalizationProfileDocumentRepository.findByUserId(u.getId()).isPresent())
                .count();
        log.info("UserProfile(임베딩) 생성된 사용자: {} 명", profileCount);

        // ID 변환: 실제 DB ID -> 원격 DB ID
        Map<Long, Map<Long, Integer>> convertedGroundTruthMap = new HashMap<>();
        userGroundTruthMap.forEach((userId, scores) -> {
            Map<Long, Integer> convertedScores = new HashMap<>();
            scores.forEach((actualPostId, score) -> {
                Long remoteId = actualToRemotePostIdMap.get(actualPostId);
                if (remoteId != null) {
                    convertedScores.put(remoteId, score);
                } else {
                    log.warn("Ground Truth Post ID 매핑 실패: actualPostId={}", actualPostId);
                }
            });
            convertedGroundTruthMap.put(userId, convertedScores);
        });

        fileExporter.writeJsonFile("ground-truth.json", convertedGroundTruthMap);
        log.info("✓ Ground Truth {} 명 export 완료 (원격 ID 변환 적용)", convertedGroundTruthMap.size());
    }

    @Test
    @Order(3)
    @DisplayName("STEP 3: 사용자 데이터를 JSON으로 Export")
    @Transactional(readOnly = true)
    void step3_ExportUserData() throws IOException {
        log.info("===== STEP 3: 사용자 데이터 Export 시작 =====");

        fileExporter.ensureOutputDirectory();

        List<User> users = exportUsers();
        log.info("✓ 사용자 {} 명 export 완료", users.size());

        List<ReadPost> readPosts = exportReadPosts(users);
        log.info("✓ 읽은 글 이력 {} 개 export 완료", readPosts.size());

        List<PersonalizationProfileDocument> userProfiles = exportUserProfiles(users);
        log.info("✓ PersonalizationProfileDocument {} 개 export 완료 (임베딩 포함)", userProfiles.size());

        log.info("===== STEP 3 완료 =====");
        log.info("출력 위치: {}", fileExporter.getOutputDir());
        log.info("\n생성된 파일:");
        log.info("  - users.json ({} 명)", users.size());
        log.info("  - read-posts.json ({} 개)", readPosts.size());
        log.info("  - user-profiles.json ({} 개, profileVector 3072차원 포함)", userProfiles.size());
        log.info("\nSTEP 2에서 이미 생성된 파일:");
        log.info("  - ground-truth.json (사용자별 정답 게시글 ID + 관련도 점수)");
    }

    private List<User> exportUsers() throws IOException {
        List<User> users = userRepository.findAll();

        // DTO 변환 (순환 참조 방지)
        List<Map<String, Object>> userDtos = users.stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());

        fileExporter.writeJsonFile("users.json", userDtos);
        return users;
    }

    private List<ReadPost> exportReadPosts(List<User> users) throws IOException {
        List<ReadPost> allReadPosts = new ArrayList<>();

        for (User user : users) {
            List<ReadPost> userReadPosts = readPostRepository
                    .findRecentReadPostsByUserIdWithMinDuration(
                            user.getId(),
                            org.springframework.data.domain.Pageable.unpaged()
                    );
            allReadPosts.addAll(userReadPosts);
        }

        // DTO 변환
        List<Map<String, Object>> readPostDtos = allReadPosts.stream()
                .map(this::convertReadPostToDto)
                .collect(Collectors.toList());

        fileExporter.writeJsonFile("read-posts.json", readPostDtos);
        return allReadPosts;
    }

    private List<PersonalizationProfileDocument> exportUserProfiles(List<User> users) throws IOException {
        List<PersonalizationProfileDocument> profiles = new ArrayList<>();
        int notFoundCount = 0;

        for (User user : users) {
            Optional<PersonalizationProfileDocument> profileOpt =
                    personalizationProfileDocumentRepository.findByUserId(user.getId());

            if (profileOpt.isPresent()) {
                profiles.add(profileOpt.get());
            } else {
                notFoundCount++;
                log.warn("PersonalizationProfileDocument not found for userId: {}", user.getId());
            }
        }

        if (notFoundCount > 0) {
            log.warn("총 {} 명의 PersonalizationProfileDocument를 찾지 못했습니다.", notFoundCount);
        }

        // DTO 변환 (profileVector는 float[]이므로 List로 변환)
        List<Map<String, Object>> profileDtos = profiles.stream()
                .map(this::convertUserProfileToDto)
                .collect(Collectors.toList());

        fileExporter.writeJsonFile("user-profiles.json", profileDtos);

        // 임베딩 차원 검증
        if (!profiles.isEmpty()) {
            PersonalizationProfileDocument sample = profiles.get(0);
            log.info("임베딩 차원 검증:");
            log.info("  - profileVector: {} 차원",
                    sample.getProfileVector() != null ? sample.getProfileVector().length : "null");
        }

        return profiles;
    }

    private Map<String, Object> convertUserToDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("email", user.getEmail());
        dto.put("nickname", user.getNickName());
        dto.put("profileImageUrl", user.getProfileImage());
        dto.put("socialType", user.getSocialType().name());
        dto.put("socialId", user.getSocialId());
        dto.put("role", user.getRole().name());

        // 관심사 카테고리
        if (user.getInterestCategories() != null) {
            List<Map<String, Object>> interests = user.getInterestCategories().stream()
                    .map(ic -> {
                        Map<String, Object> interestDto = new HashMap<>();
                        interestDto.put("category", ic.getCategory().name());

                        // 키워드
                        if (ic.getKeywords() != null) {
                            List<String> keywords = ic.getKeywords().stream()
                                    .map(k -> k.getKeyword().name())
                                    .collect(Collectors.toList());
                            interestDto.put("keywords", keywords);
                        }

                        return interestDto;
                    })
                    .collect(Collectors.toList());
            dto.put("interests", interests);
        }

        return dto;
    }

    private Map<String, Object> convertReadPostToDto(ReadPost readPost) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("userId", readPost.getUser().getId());

        // 실제 DB ID -> 원격 DB ID로 변환
        Long actualPostId = readPost.getPost().getId();
        Long remotePostId = actualToRemotePostIdMap.get(actualPostId);
        dto.put("postId", remotePostId != null ? remotePostId : actualPostId);

        dto.put("readAt", readPost.getReadAt().toString());
        dto.put("readDurationSeconds", readPost.getReadDurationSeconds());
        return dto;
    }

    private Map<String, Object> convertUserProfileToDto(PersonalizationProfileDocument profile) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", profile.getId());
        dto.put("userId", profile.getUserId());
        dto.put("profileText", profile.getProfileText());

        // float[] -> List<Float> 변환
        if (profile.getProfileVector() != null) {
            List<Float> vectorList = new ArrayList<>();
            for (float v : profile.getProfileVector()) {
                vectorList.add(v);
            }
            dto.put("profileVector", vectorList);
        }

        dto.put("interests", profile.getInterests());
        dto.put("keyKeywords", profile.getKeyKeywords());

        return dto;
    }
}
