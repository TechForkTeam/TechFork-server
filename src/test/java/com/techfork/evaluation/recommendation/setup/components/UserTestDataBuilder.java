package com.techfork.evaluation.recommendation.setup.components;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.bookmark.entity.Bookmark;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.bookmark.repository.BookmarkRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.entity.UserInterestCategory;
import com.techfork.domain.useraccount.entity.UserInterestKeyword;
import com.techfork.domain.useraccount.enums.EInterestCategory;
import com.techfork.domain.useraccount.enums.EInterestKeyword;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserInterestCategoryRepository;
import com.techfork.domain.useraccount.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTestDataBuilder {

    private final UserRepository userRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final ReadPostRepository readPostRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    public User createUserWithInterests(List<EInterestCategory> interestCategories) {
        User user = User.createSocialUser(
                SocialType.KAKAO,
                "testSocialId_" + UUID.randomUUID(),
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

    public void createReadPosts(User user, List<Post> posts) {
        LocalDateTime now = LocalDateTime.now();
        List<ReadPost> readPosts = new ArrayList<>();

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            ReadPost readPost = ReadPost.create(
                    user,
                    post,
                    now.minusDays(posts.size() - i),
                    180 // 3분 읽음
            );
            readPosts.add(readPost);
        }

        readPostRepository.saveAll(readPosts);
        log.debug("읽은 글 {} 개 생성 완료", posts.size());
    }


    public void createBookmarks(User user, List<Post> readPosts, int bookmarkCount) {
        LocalDateTime now = LocalDateTime.now();

        List<Post> postsToBookmark = readPosts.stream()
                .distinct()
                .collect(Collectors.toList());
        Collections.shuffle(postsToBookmark);

        int actualBookmarkCount = Math.min(bookmarkCount, postsToBookmark.size());

        List<Bookmark> bookmarks = new ArrayList<>();
        for (int i = 0; i < actualBookmarkCount; i++) {
            Post post = postsToBookmark.get(i);
            Bookmark bookmark = Bookmark.create(
                    user,
                    post,
                    now.minusDays(readPosts.size() - i - 5) // 읽은 시점보다 약간 후에 북마크
            );
            bookmarks.add(bookmark);
        }

        bookmarkRepository.saveAll(bookmarks);
        log.debug("북마크한 글 {} 개 생성 완료", actualBookmarkCount);
    }

    public void createSearchHistories(User user, List<String> searchKeywords, int searchHistoryCount) {
        LocalDateTime now = LocalDateTime.now();
        List<SearchHistory> searchHistories = new ArrayList<>();

        for (int i = 0; i < searchHistoryCount; i++) {
            String query = searchKeywords.get(i % searchKeywords.size());
            SearchHistory searchHistory = SearchHistory.create(
                    user,
                    query,
                    now.minusDays(searchHistoryCount - i * 2) // 읽기 활동 사이사이에 검색
            );
            searchHistories.add(searchHistory);
        }

        searchHistoryRepository.saveAll(searchHistories);
        log.debug("검색 기록 {} 개 생성 완료", searchHistoryCount);
    }
}
