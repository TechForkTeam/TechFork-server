package com.techfork.evaluation.recommendation.setup.components;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.user.enums.EInterestCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관심사 카테고리 기반 게시글 매칭
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostMatcher {

    private final PostRepository postRepository;

    // 관심사 카테고리별 키워드 매핑
    private static final Map<EInterestCategory, List<String>> INTEREST_KEYWORDS = createKeywordMap();

    private static Map<EInterestCategory, List<String>> createKeywordMap() {
        Map<EInterestCategory, List<String>> map = new HashMap<>();

        map.put(EInterestCategory.BACKEND, Arrays.asList(
                "Spring", "Java", "Kotlin", "API", "서버", "Backend", "백엔드", "JPA", "Hibernate"
        ));

        map.put(EInterestCategory.AI_ML, Arrays.asList(
                "AI", "ML", "머신러닝", "딥러닝", "LLM", "GPT", "인공지능", "모델", "학습"
        ));

        map.put(EInterestCategory.FRONTEND, Arrays.asList(
                "React", "Vue", "JavaScript", "CSS", "UI", "Frontend", "프론트엔드",
                "TypeScript", "HTML", "웹", "브라우저", "Node.js", "Next", "Angular",
                "Webpack", "번들", "SPA", "SSR", "렌더링", "컴포넌트", "디자인"
        ));

        map.put(EInterestCategory.DATA_ENGINEERING, Arrays.asList(
                "데이터", "분석", "Spark", "Kafka", "파이프라인", "ETL"
        ));

        map.put(EInterestCategory.DATA_SCIENCE, Arrays.asList(
                "데이터", "분석", "ML", "통계", "시각화", "예측"
        ));

        map.put(EInterestCategory.DATABASE, Arrays.asList(
                "SQL", "Database", "MySQL", "PostgreSQL", "DB", "쿼리", "인덱스"
        ));

        map.put(EInterestCategory.DEVOPS, Arrays.asList(
                "DevOps", "Docker", "Kubernetes", "CI/CD", "배포", "인프라"
        ));

        map.put(EInterestCategory.CLOUD, Arrays.asList(
                "AWS", "클라우드", "Cloud", "Azure", "GCP"
        ));

        return map;
    }

    /**
     * 관심사와 관련된 게시글 찾기 (제목 기반 키워드 매칭)
     */
    @Transactional(readOnly = true)
    public List<Post> findPostsRelatedToInterests(List<EInterestCategory> interests, int limit) {
        List<Post> allPosts = postRepository.findAll();

        // 관심사 키워드와 매칭되는 게시글 찾기
        List<Post> relatedPosts = allPosts.stream()
                .filter(post -> matchesInterests(post, interests))
                .limit(limit)
                .collect(Collectors.toList());

        // 매칭 안 되면 랜덤으로 채우기
        if (relatedPosts.size() < limit && !allPosts.isEmpty()) {
            return fillWithRandomPosts(relatedPosts, allPosts, limit);
        }

        return relatedPosts;
    }

    /**
     * 게시글이 관심사와 매칭되는지 확인
     */
    private boolean matchesInterests(Post post, List<EInterestCategory> interests) {
        String title = post.getTitle().toLowerCase();

        return interests.stream()
                .flatMap(interest -> INTEREST_KEYWORDS.getOrDefault(interest, Collections.emptyList()).stream())
                .anyMatch(keyword -> title.contains(keyword.toLowerCase()));
    }

    /**
     * 부족한 만큼 랜덤 게시글로 채우기
     */
    private List<Post> fillWithRandomPosts(List<Post> relatedPosts, List<Post> allPosts, int limit) {
        List<Post> remaining = new ArrayList<>(allPosts);
        remaining.removeAll(relatedPosts);
        Collections.shuffle(remaining);

        List<Post> combined = new ArrayList<>(relatedPosts);
        int needed = limit - relatedPosts.size();
        int available = Math.min(needed, remaining.size());
        combined.addAll(remaining.subList(0, available));

        return combined;
    }

    /**
     * 특정 관심사 카테고리의 키워드 목록 반환
     */
    public List<String> getKeywordsForCategory(EInterestCategory category) {
        return INTEREST_KEYWORDS.getOrDefault(category, Collections.emptyList());
    }
}
