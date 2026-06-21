package com.techfork.domain.recommendation.repository;

import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RecommendedPostRepository 테스트")
class RecommendedPostRepositoryTest {

    @Autowired
    private RecommendedPostRepository recommendedPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private TechBlog testBlog;
    private Post post1;
    private Post post2;
    private Post post3;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.createSocialUser(
                SocialType.KAKAO,
                "recommendation-repository-user",
                "recommendation@example.com",
                "profile.jpg"
        ));

        testBlog = techBlogRepository.save(TechBlog.create(
                "테스트회사",
                "https://recommendation-test.com",
                "https://recommendation-test.com/rss",
                "https://recommendation-test.com/logo.png"
        ));

        post1 = postRepository.save(createPost("게시글 1", "https://recommendation-test.com/posts/1"));
        post2 = postRepository.save(createPost("게시글 2", "https://recommendation-test.com/posts/2"));
        post3 = postRepository.save(createPost("게시글 3", "https://recommendation-test.com/posts/3"));
    }

    @AfterEach
    void tearDown() {
        recommendedPostRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자의 현재 추천 목록을 rank 오름차순으로 조회한다")
    void findByUserOrderByRankAsc_ReturnsRecommendationsOrderedByRank() {
        RecommendedPost rank3 = RecommendedPost.create(testUser, post3, 0.7, 0.65, 3);
        RecommendedPost rank1 = RecommendedPost.create(testUser, post1, 0.9, 0.85, 1);
        RecommendedPost rank2 = RecommendedPost.create(testUser, post2, 0.8, 0.75, 2);
        recommendedPostRepository.saveAll(List.of(rank3, rank1, rank2));

        entityManager.flush();
        entityManager.clear();

        List<RecommendedPost> result = recommendedPostRepository.findByUserOrderByRankAsc(testUser);

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(RecommendedPost::getRankOrder)
                .containsExactly(1, 2, 3);
        assertThat(result)
                .extracting(recommendedPost -> recommendedPost.getPost().getId())
                .containsExactly(post1.getId(), post2.getId(), post3.getId());
    }

    @Test
    @DisplayName("deleteByUser는 지정한 사용자의 현재 추천만 삭제한다")
    void deleteByUser_DeletesOnlyRecommendationsForGivenUser() {
        User anotherUser = userRepository.save(User.createSocialUser(
                SocialType.KAKAO,
                "recommendation-repository-another-user",
                "another-recommendation@example.com",
                "another-profile.jpg"
        ));
        recommendedPostRepository.saveAll(List.of(
                RecommendedPost.create(testUser, post1, 0.9, 0.85, 1),
                RecommendedPost.create(testUser, post2, 0.8, 0.75, 2),
                RecommendedPost.create(anotherUser, post1, 0.7, 0.65, 1)
        ));
        entityManager.flush();

        recommendedPostRepository.deleteByUser(testUser);
        entityManager.flush();
        entityManager.clear();

        assertThat(recommendedPostRepository.findByUserOrderByRankAsc(testUser)).isEmpty();
        assertThat(recommendedPostRepository.findByUserOrderByRankAsc(anotherUser))
                .extracting(recommendedPost -> recommendedPost.getPost().getId())
                .containsExactly(post1.getId());
    }

    @Test
    @DisplayName("같은 사용자와 게시글 조합은 현재 추천에 중복 저장할 수 없다")
    void save_DuplicateUserAndPostCombination_ThrowsException() {
        RecommendedPost firstRecommendation = RecommendedPost.create(testUser, post1, 0.9, 0.85, 1);
        RecommendedPost duplicateRecommendation = RecommendedPost.create(testUser, post1, 0.8, 0.75, 2);

        recommendedPostRepository.saveAndFlush(firstRecommendation);

        assertThatThrownBy(() -> recommendedPostRepository.saveAndFlush(duplicateRecommendation))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();
    }

    private Post createPost(String title, String url) {
        return Post.builder()
                .title(title)
                .fullContent(title + " 전체 내용")
                .plainContent(title + " 내용")
                .company(testBlog.getCompanyName())
                .url(url)
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
    }
}
