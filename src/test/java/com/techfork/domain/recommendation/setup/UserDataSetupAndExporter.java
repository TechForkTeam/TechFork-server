package com.techfork.domain.recommendation.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.recommendation.setup.components.TestDataGenerator;
import com.techfork.domain.recommendation.util.EvaluationFixtureLoader;
import com.techfork.domain.recommendation_quality.UserCreationResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Testcontainer에서 사용자 데이터 생성 및 JSON으로 export
 *
 * 실행 방법:
 * ./gradlew test --tests UserDataSetupAndExporter
 *
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
    private UserProfileDocumentRepository userProfileDocumentRepository;

    @Autowired
    private TestDataGenerator testDataGenerator;

    @Autowired
    private EvaluationFixtureLoader fixtureLoader;

    private static final String OUTPUT_DIR = "src/test/resources/fixtures/evaluation";

    // Ground Truth 저장용 (사용자 ID -> (게시글 ID -> 관련도 점수))
    private final Map<Long, Map<Long, Integer>> userGroundTruthMap = new HashMap<>();
    private static final int USER_COUNT = 5;
    private static final int READ_POST_COUNT = 80;   // 프로필 구성용 (읽은 글) - 1100개 데이터셋 기준 (약 7%)
    private static final int HOLDOUT_COUNT = 30;     // Ground Truth (평가용, 숨김) - 평가 샘플 (약 2.7%)

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @Order(1)
    @DisplayName("STEP 1: 게시글 픽스처 로드 (posts.json, post-documents.json)")
    void step1_LoadPostFixtures() {
        log.info("===== STEP 1: 게시글 픽스처 로드 =====");
        log.info("주의: PostDataExporter를 먼저 실행하여 게시글 데이터를 export해야 합니다.");

        try {
            // 게시글 데이터만 로드 (사용자 데이터는 제외)
            fixtureLoader.loadPostsOnly();
            log.info("✓ 게시글 픽스처 로드 완료");
        } catch (Exception e) {
            log.error("게시글 픽스처 로드 실패. PostDataExporter를 먼저 실행하세요.", e);
            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName("STEP 2: 테스트 사용자 5명 생성 (임베딩 포함)")
    void step2_CreateTestUsers() {
        log.info("===== STEP 2: 테스트 사용자 생성 =====");

        // 공통 읽은 글 풀 초기화
        testDataGenerator.resetSharedReadPosts();

        List<List<EInterestCategory>> interestCombos = Arrays.asList(
                Arrays.asList(EInterestCategory.BACKEND),
                Arrays.asList(EInterestCategory.FRONTEND),
                Arrays.asList(EInterestCategory.AI_ML),
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.DATABASE),
                Arrays.asList(EInterestCategory.AI_ML, EInterestCategory.DATA_SCIENCE)
        );

        for (int i = 0; i < USER_COUNT; i++) {
            List<EInterestCategory> interests = interestCombos.get(i);

            log.info("사용자 {}/{} 생성 중... (관심사: {})", i + 1, USER_COUNT, interests);

            UserCreationResult result = testDataGenerator.createTestUserWithGroundTruth(interests, READ_POST_COUNT, HOLDOUT_COUNT);
            User user = result.getUser();

            // Ground Truth 점수 저장
            userGroundTruthMap.put(user.getId(), result.getGroundTruthScores());

            log.info("✓ 사용자 생성 완료: ID={}, 관심사={}, 읽은 글={} 개, Ground Truth={} 개",
                    user.getId(), interests, READ_POST_COUNT, result.getGroundTruthScores().size());
        }

        log.info("===== STEP 2 완료: {} 명 사용자 생성 완료 =====\n", USER_COUNT);

        // 생성된 사용자 검증
        List<User> users = userRepository.findAll();
        log.info("총 생성된 사용자: {} 명", users.size());

        // UserProfile 검증
        long profileCount = users.stream()
                .filter(u -> userProfileDocumentRepository.findByUserId(u.getId()).isPresent())
                .count();
        log.info("UserProfile(임베딩) 생성된 사용자: {} 명", profileCount);

        // Ground Truth export (STEP 2에서 바로 저장)
        try {
            Map<Long, Map<Long, Integer>> groundTruth = exportGroundTruth(users);
            log.info("✓ Ground Truth {} 명 export 완료", groundTruth.size());
        } catch (Exception e) {
            log.error("Ground Truth export 실패", e);
        }
    }

    @Test
    @Order(3)
    @DisplayName("STEP 3: 사용자 데이터를 JSON으로 Export")
    @Transactional(readOnly = true)
    void step3_ExportUserData() throws IOException {
        log.info("===== STEP 3: 사용자 데이터 Export 시작 =====");

        // 출력 디렉토리 확인
        Path outputPath = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputPath);
        log.info("출력 디렉토리: {}", outputPath.toAbsolutePath());

        // 1. 사용자 데이터 export
        List<User> users = exportUsers();
        log.info("✓ 사용자 {} 명 export 완료", users.size());

        // 2. 읽은 글 이력 export
        List<ReadPost> readPosts = exportReadPosts(users);
        log.info("✓ 읽은 글 이력 {} 개 export 완료", readPosts.size());

        // 3. UserProfileDocument (임베딩 포함) export
        List<UserProfileDocument> userProfiles = exportUserProfiles(users);
        log.info("✓ UserProfileDocument {} 개 export 완료 (임베딩 포함)", userProfiles.size());

        log.info("===== STEP 3 완료 =====");
        log.info("출력 위치: {}", outputPath.toAbsolutePath());
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

        writeJsonFile("users.json", userDtos);
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

        writeJsonFile("read-posts.json", readPostDtos);
        return allReadPosts;
    }

    private List<UserProfileDocument> exportUserProfiles(List<User> users) throws IOException {
        List<UserProfileDocument> profiles = new ArrayList<>();
        int notFoundCount = 0;

        for (User user : users) {
            Optional<UserProfileDocument> profileOpt =
                    userProfileDocumentRepository.findByUserId(user.getId());

            if (profileOpt.isPresent()) {
                profiles.add(profileOpt.get());
            } else {
                notFoundCount++;
                log.warn("UserProfileDocument not found for userId: {}", user.getId());
            }
        }

        if (notFoundCount > 0) {
            log.warn("총 {} 명의 UserProfileDocument를 찾지 못했습니다.", notFoundCount);
        }

        // DTO 변환 (profileVector는 float[]이므로 List로 변환)
        List<Map<String, Object>> profileDtos = profiles.stream()
                .map(this::convertUserProfileToDto)
                .collect(Collectors.toList());

        writeJsonFile("user-profiles.json", profileDtos);

        // 임베딩 차원 검증
        if (!profiles.isEmpty()) {
            UserProfileDocument sample = profiles.get(0);
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
        dto.put("postId", readPost.getPost().getId());
        dto.put("readAt", readPost.getReadAt().toString());
        dto.put("readDurationSeconds", readPost.getReadDurationSeconds());
        return dto;
    }

    private Map<String, Object> convertUserProfileToDto(UserProfileDocument profile) {
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

        return dto;
    }

    private Map<Long, Map<Long, Integer>> exportGroundTruth(List<User> users) throws IOException {
        // 사용자 ID -> (게시글 ID -> 관련도 점수)
        // userGroundTruthMap에 이미 저장되어 있음 (STEP 2에서 생성)

        if (userGroundTruthMap.isEmpty()) {
            log.warn("Ground Truth가 비어있습니다. STEP 2가 먼저 실행되어야 합니다.");
        }

        writeJsonFile("ground-truth.json", userGroundTruthMap);

        // 통계 로깅
        userGroundTruthMap.forEach((userId, scores) -> {
            log.debug("User {}: {} 개 정답 게시글", userId, scores.size());

            // 점수 분포
            Map<Integer, Long> distribution = scores.values().stream()
                    .collect(Collectors.groupingBy(score -> score, Collectors.counting()));
            log.debug("  점수 분포: {}", distribution);
        });

        return userGroundTruthMap;
    }

    private void writeJsonFile(String filename, Object data) throws IOException {
        File outputFile = new File(OUTPUT_DIR, filename);
        objectMapper.writeValue(outputFile, data);
        log.debug("파일 작성: {}", outputFile.getAbsolutePath());
    }
}
