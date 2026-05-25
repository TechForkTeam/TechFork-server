package com.techfork.evaluation.recommendation.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostDocumentRepository;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.personalization.document.PersonalizationProfileDocument;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.entity.UserInterestCategory;
import com.techfork.useraccount.entity.UserInterestKeyword;
import com.techfork.useraccount.enums.EInterestCategory;
import com.techfork.useraccount.enums.EInterestKeyword;
import com.techfork.useraccount.enums.Role;
import com.techfork.useraccount.enums.SocialType;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.useraccount.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON fixture를 읽어서 Testcontainer에 로드하는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationFixtureLoader {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReadPostRepository readPostRepository;
    private final PostDocumentRepository postDocumentRepository;
    private final PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;
    private final TechBlogRepository techBlogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String FIXTURE_PATH = "fixtures/evaluation/";

    @Transactional
    public Map<Long, Map<Long, Integer>> loadAll() {
        log.info("===== Fixture 로드 시작 =====");

        try {
            Map<Long, User> userMap = loadUsers();
            log.info("✓ 사용자 {} 명 로드 완료", userMap.size());

            Map<Long, Post> postMap = loadPosts();
            log.info("✓ 게시글 {} 개 로드 완료", postMap.size());

            int readPostCount = loadReadPosts(userMap, postMap);
            log.info("✓ 읽은 글 이력 {} 개 로드 완료", readPostCount);

            int postDocCount = loadPostDocuments(postMap);
            log.info("✓ PostDocument {} 개 로드 완료 (임베딩 포함)", postDocCount);

            int userProfileCount = loadUserProfiles(userMap);
            log.info("✓ PersonalizationProfileDocument {} 개 로드 완료 (임베딩 포함)", userProfileCount);

            Map<Long, Map<Long, Integer>> groundTruth = loadGroundTruth(userMap, postMap);
            log.info("✓ Ground Truth {} 명 사용자 로드 완료", groundTruth.size());

            log.info("===== Fixture 로드 완료 =====\n");

            return groundTruth;

        } catch (IOException e) {
            log.error("Fixture 로드 실패", e);
            throw new RuntimeException("Fixture 로드 중 오류 발생", e);
        }
    }

    @Transactional
    public Map<Long, Post> loadPostsOnly() {
        log.info("===== 게시글 Fixture 로드 시작 =====");

        try {
            Map<Long, Post> postMap = loadPosts();
            log.info("✓ 게시글 {} 개 로드 완료", postMap.size());

            int postDocCount = loadPostDocuments(postMap);
            log.info("✓ PostDocument {} 개 로드 완료 (임베딩 포함)", postDocCount);

            log.info("===== 게시글 Fixture 로드 완료 =====\n");
            return postMap;

        } catch (IOException e) {
            log.error("게시글 Fixture 로드 실패", e);
            throw new RuntimeException("게시글 Fixture 로드 중 오류 발생", e);
        }
    }

    private Map<Long, User> loadUsers() throws IOException {
        List<Map<String, Object>> userDtos = readJsonFile("users.json", new TypeReference<>() {
        });

        Map<Long, User> userMap = new HashMap<>();

        for (Map<String, Object> dto : userDtos) {
            Long originalUserId = ((Number) dto.get("id")).longValue(); // JSON의 원래 ID
            String email = (String) dto.get("email");
            String nickname = (String) dto.get("nickname");
            String profileImageUrl = (String) dto.get("profileImageUrl");
            String socialType = (String) dto.get("socialType");
            String socialId = (String) dto.get("socialId");
            String roleStr = (String) dto.get("role");

            User user = User.builder()
                    .email(email)
                    .nickName(nickname)
                    .profileImage(profileImageUrl)
                    .socialType(SocialType.valueOf(socialType))
                    .socialId(socialId)
                    .role(Role.valueOf(roleStr))
                    .build();

            user = userRepository.save(user);

            // 관심사 추가
            List<Map<String, Object>> interests =
                    (List<Map<String, Object>>) dto.get("interests");

            if (interests != null) {
                for (Map<String, Object> interestDto : interests) {
                    String categoryStr = (String) interestDto.get("category");
                    EInterestCategory category = EInterestCategory.valueOf(categoryStr);

                    UserInterestCategory interestCategory =
                            UserInterestCategory.create(user, category);

                    // 키워드 추가
                    List<String> keywords = (List<String>) interestDto.get("keywords");
                    if (keywords != null) {
                        for (String keywordStr : keywords) {
                            EInterestKeyword keyword = EInterestKeyword.valueOf(keywordStr);
                            UserInterestKeyword interestKeyword =
                                    UserInterestKeyword.create(interestCategory, keyword);
                            interestCategory.addKeyword(interestKeyword);
                        }
                    }

                    user.getInterestCategories().add(interestCategory);
                }

                user = userRepository.save(user);
            }

            // JSON의 원래 ID를 키로 사용 (Ground-Truth, ReadPost 매핑을 위해)
            userMap.put(originalUserId, user);
        }

        return userMap;
    }

    private Map<Long, Post> loadPosts() throws IOException {
        List<Map<String, Object>> postDtos = readJsonFile("posts.json", new TypeReference<>() {
        });

        Map<Long, Post> postMap = new HashMap<>();

        // TechBlog ID -> TechBlog 매핑
        Map<Long, TechBlog> techBlogMap = new HashMap<>();

        for (Map<String, Object> dto : postDtos) {
            Long originalPostId = ((Number) dto.get("id")).longValue(); // JSON의 원래 ID
            Long techBlogId = ((Number) dto.get("techBlogId")).longValue();

            // TechBlog 생성 또는 조회
            TechBlog techBlog = techBlogMap.get(techBlogId);
            if (techBlog == null) {
                String companyName = (String) dto.get("techBlogCompanyName");
                String blogUrl = (String) dto.get("techBlogUrl");
                String rssUrl = (String) dto.get("techBlogRssUrl");

                techBlog = TechBlog.create(companyName, blogUrl, rssUrl, null);
                techBlog = techBlogRepository.save(techBlog);
                techBlogMap.put(techBlogId, techBlog);
            }

            // Post 생성
            String title = (String) dto.get("title");
            String url = (String) dto.get("url");
            String summary = (String) dto.get("summary");
            String shortSummary = (String) dto.get("shortSummary");
            String company = (String) dto.get("company");
            String logoUrl = (String) dto.get("logoUrl");
            String thumbnailUrl = (String) dto.get("thumbnailUrl");
            String publishedAtStr = (String) dto.get("publishedAt");
            LocalDateTime publishedAt = publishedAtStr != null ?
                    LocalDateTime.parse(publishedAtStr) : null;

            Post post = Post.builder()
                    .title(title)
                    .url(url)
                    .summary(summary)
                    .shortSummary(shortSummary)
                    .company(company)
                    .logoUrl(logoUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .techBlog(techBlog)
                    .build();

            post = postRepository.save(post);
            // JSON의 원래 ID를 키로 사용 (PostDocument 매핑을 위해)
            postMap.put(originalPostId, post);
        }

        return postMap;
    }

    private int loadReadPosts(Map<Long, User> userMap, Map<Long, Post> postMap)
            throws IOException {
        List<Map<String, Object>> readPostDtos = readJsonFile("read-posts.json", new TypeReference<>() {
        });

        int count = 0;

        for (Map<String, Object> dto : readPostDtos) {
            Long userId = ((Number) dto.get("userId")).longValue();
            Long postId = ((Number) dto.get("postId")).longValue();
            String readAtStr = (String) dto.get("readAt");
            Integer readDurationSeconds = ((Number) dto.get("readDurationSeconds")).intValue();

            User user = userMap.get(userId);
            Post post = postMap.get(postId);

            if (user == null || post == null) {
                log.warn("ReadPost 로드 실패: userId={}, postId={}", userId, postId);
                continue;
            }

            LocalDateTime readAt = LocalDateTime.parse(readAtStr);

            ReadPost readPost = ReadPost.create(user, post, readAt, readDurationSeconds);
            readPostRepository.save(readPost);
            count++;
        }

        return count;
    }

    private int loadPostDocuments(Map<Long, Post> postMap) throws IOException {
        List<Map<String, Object>> docDtos = readJsonFile("post-documents.json", new TypeReference<>() {
        });

        int count = 0;

        for (Map<String, Object> dto : docDtos) {
            String id = String.valueOf(dto.get("id"));
            Long originalPostId = ((Number) dto.get("postId")).longValue();

            // JSON의 원래 Post ID를 실제 저장된 Post ID로 매핑
            Post post = postMap.get(originalPostId);
            if (post == null) {
                log.warn("PostDocument 로드 중 Post를 찾을 수 없음: originalPostId={}", originalPostId);
                continue;
            }
            Long actualPostId = post.getId();

            String title = (String) dto.get("title");
            String summary = (String) dto.get("summary");
            String shortSummary = (String) dto.get("shortSummary");
            String company = (String) dto.get("company");
            String url = (String) dto.get("url");
            String logoUrl = (String) dto.get("logoUrl");
            String thumbnailUrl = (String) dto.get("thumbnailUrl");
            String publishedAt = (String) dto.get("publishedAt");

            // 임베딩 벡터 (List<Number> -> List<Float>)
            List<Float> titleEmbedding = convertToFloatList(
                    (List<Number>) dto.get("titleEmbedding"));
            List<Float> summaryEmbedding = convertToFloatList(
                    (List<Number>) dto.get("summaryEmbedding"));

            // ContentChunk (nested)
            List<ContentChunk> contentChunks = null;
            if (dto.get("contentChunks") != null) {
                List<Map<String, Object>> chunkDtos =
                        (List<Map<String, Object>>) dto.get("contentChunks");
                contentChunks = chunkDtos.stream()
                        .map(this::convertToContentChunk)
                        .collect(Collectors.toList());
            }

            PostDocument postDoc = PostDocument.builder()
                    .id(id)
                    .postId(actualPostId)  // 실제 저장된 Post ID 사용
                    .title(title)
                    .summary(summary)
                    .shortSummary(shortSummary)
                    .company(company)
                    .url(url)
                    .logoUrl(logoUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .publishedAtString(publishedAt)
                    .titleEmbedding(titleEmbedding)
                    .summaryEmbedding(summaryEmbedding)
                    .contentChunks(contentChunks)
                    .build();

            postDocumentRepository.save(postDoc);
            count++;
        }

        return count;
    }

    private int loadUserProfiles(Map<Long, User> userMap) throws IOException {
        List<Map<String, Object>> profileDtos = readJsonFile("user-profiles.json", new TypeReference<>() {
        });

        int count = 0;

        for (Map<String, Object> dto : profileDtos) {
            Long originalUserId = ((Number) dto.get("userId")).longValue();
            String profileText = (String) dto.get("profileText");
            List<String> interests = (List<String>) dto.get("interests");
            List<String> keyKeywords = (List<String>) dto.get("keyKeywords");

            // JSON의 원래 User ID를 실제 DB User ID로 매핑
            User user = userMap.get(originalUserId);
            if (user == null) {
                log.warn("UserProfile 로드 실패: 사용자를 찾을 수 없음 (원본 User ID={})", originalUserId);
                continue;
            }
            Long actualUserId = user.getId();

            // 임베딩 벡터 (List<Number> -> float[])
            List<Number> vectorList = (List<Number>) dto.get("profileVector");
            float[] profileVector = null;
            if (vectorList != null) {
                profileVector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    profileVector[i] = vectorList.get(i).floatValue();
                }
            }

            PersonalizationProfileDocument profile = PersonalizationProfileDocument.builder()
                    .userId(actualUserId)
                    .profileText(profileText)
                    .profileVector(profileVector)
                    .interests(interests)
                    .keyKeywords(keyKeywords)
                    .build();

            personalizationProfileDocumentRepository.save(profile);
            count++;
        }

        return count;
    }

    private ContentChunk convertToContentChunk(Map<String, Object> dto) {
        Integer chunkOrder = dto.get("chunkOrder") != null ?
                ((Number) dto.get("chunkOrder")).intValue() : null;
        String chunkText = (String) dto.get("chunkText");
        List<Float> embedding = convertToFloatList((List<Number>) dto.get("embedding"));

        return ContentChunk.builder()
                .chunkOrder(chunkOrder)
                .chunkText(chunkText)
                .embedding(embedding)
                .build();
    }

    private List<Float> convertToFloatList(List<Number> numbers) {
        if (numbers == null) return null;
        return numbers.stream()
                .map(Number::floatValue)
                .collect(Collectors.toList());
    }

    /**
     * Ground Truth 데이터 로드 (Post ID를 실제 DB ID로 매핑)
     * JSON 구조: { "userId": { "postId": relevanceScore, ... }, ... }
     *
     * @param userMap JSON의 원래 User ID -> 실제 저장된 User 매핑
     * @param postMap JSON의 원래 Post ID -> 실제 저장된 Post 매핑
     * @return Map<실제 사용자 DB ID, Map<실제 게시글 DB ID, 관련도점수>>
     */
    private Map<Long, Map<Long, Integer>> loadGroundTruth(
            Map<Long, User> userMap,
            Map<Long, Post> postMap) throws IOException {
        // JSON에서 String 키로 읽어서 Long으로 변환
        Map<String, Map<String, Integer>> rawData = readJsonFile("ground-truth.json", new TypeReference<>() {
        });

        Map<Long, Map<Long, Integer>> groundTruth = new HashMap<>();
        int mappedCount = 0;
        int skippedCount = 0;

        for (Map.Entry<String, Map<String, Integer>> userEntry : rawData.entrySet()) {
            Long originalUserId = Long.parseLong(userEntry.getKey());

            // JSON의 User ID -> 실제 DB User ID 매핑
            User user = userMap.get(originalUserId);
            if (user == null) {
                log.warn("Ground Truth 로드 실패: 사용자를 찾을 수 없음 (원본 User ID={})", originalUserId);
                skippedCount++;
                continue;
            }
            Long actualUserId = user.getId();

            Map<Long, Integer> postScores = new HashMap<>();

            for (Map.Entry<String, Integer> postEntry : userEntry.getValue().entrySet()) {
                Long originalPostId = Long.parseLong(postEntry.getKey());
                Integer relevanceScore = postEntry.getValue();

                // JSON의 Post ID -> 실제 DB Post ID 매핑
                Post post = postMap.get(originalPostId);
                if (post == null) {
                    log.debug("Ground Truth 매핑 실패: Post를 찾을 수 없음 (원본 Post ID={})", originalPostId);
                    skippedCount++;
                    continue;
                }
                Long actualPostId = post.getId();

                postScores.put(actualPostId, relevanceScore);
                mappedCount++;
            }

            if (!postScores.isEmpty()) {
                groundTruth.put(actualUserId, postScores);
            }
        }

        log.info("✓ Ground Truth 매핑 완료: {} 개 (스킵: {} 개)", mappedCount, skippedCount);
        return groundTruth;
    }

    private <T> T readJsonFile(String filename, TypeReference<T> typeRef) throws IOException {
        ClassPathResource resource = new ClassPathResource(FIXTURE_PATH + filename);
        return objectMapper.readValue(resource.getInputStream(), typeRef);
    }
}
