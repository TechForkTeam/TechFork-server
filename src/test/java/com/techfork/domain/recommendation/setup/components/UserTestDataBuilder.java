package com.techfork.domain.recommendation.setup.components;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.entity.UserInterestKeyword;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.enums.EInterestKeyword;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserInterestCategoryRepository;
import com.techfork.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 테스트용 사용자 및 활동 데이터 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTestDataBuilder {

    private final UserRepository userRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final EntityManager entityManager;

    /**
     * 사용자 생성 (관심사 포함)
     */
    public User createUserWithInterests(List<EInterestCategory> interestCategories) {
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
            List<EInterestKeyword> availableKeywords = new ArrayList<>(
                    EInterestKeyword.getKeywordsByCategory(category)
            );
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

        return user;
    }

    /**
     * 읽은 글 생성
     */
    public void createReadPosts(User user, List<Post> posts) {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = 20;

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            ReadPost readPost = ReadPost.create(
                    user,
                    post,
                    now.minusDays(posts.size() - i),
                    180 // 3분 읽음
            );
            entityManager.persist(readPost);

            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();

        log.debug("읽은 글 {} 개 생성 완료", posts.size());
    }

    /**
     * 스크랩한 글 생성 (읽은 글 중 일부를 스크랩)
     */
    public void createScrapPosts(User user, List<Post> readPosts, int scrapCount) {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = 20;

        List<Post> postsToScrap = new ArrayList<>(readPosts);
        Collections.shuffle(postsToScrap);

        int actualScrapCount = Math.min(scrapCount, postsToScrap.size());

        for (int i = 0; i < actualScrapCount; i++) {
            Post post = postsToScrap.get(i);
            ScrabPost scrabPost = ScrabPost.create(
                    user,
                    post,
                    now.minusDays(readPosts.size() - i - 5) // 읽은 시점보다 약간 후에 스크랩
            );
            entityManager.persist(scrabPost);

            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();

        log.debug("스크랩한 글 {} 개 생성 완료", actualScrapCount);
    }

    /**
     * 검색 기록 생성 (관심사 기반 검색어)
     */
    public void createSearchHistories(User user, List<String> searchKeywords, int searchHistoryCount) {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = 20;

        for (int i = 0; i < searchHistoryCount; i++) {
            String searchWord = searchKeywords.get(i % searchKeywords.size());
            SearchHistory searchHistory = SearchHistory.create(
                    user,
                    searchWord,
                    now.minusDays(searchHistoryCount - i * 2) // 읽기 활동 사이사이에 검색
            );
            entityManager.persist(searchHistory);

            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();

        log.debug("검색 기록 {} 개 생성 완료", searchHistoryCount);
    }
}
