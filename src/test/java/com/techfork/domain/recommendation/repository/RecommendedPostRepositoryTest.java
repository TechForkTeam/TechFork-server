package com.techfork.domain.recommendation.repository;

import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.useraccount.domain.User;
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

import java.util.List;

import static com.techfork.domain.recommendation.fixture.RecommendationPostFixture.post;
import static com.techfork.domain.recommendation.fixture.RecommendedPostFixture.recommendedPost;
import static com.techfork.domain.recommendation.fixture.RecommendationPostFixture.techBlog;
import static com.techfork.domain.recommendation.fixture.RecommendationUserFixture.user;
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
        testUser = userRepository.save(user("recommendation-repository-user", "recommendation@example.com"));

        testBlog = techBlogRepository.save(techBlog("테스트회사", "https://recommendation-test.com"));

        post1 = postRepository.save(post(testBlog, "게시글 1", "https://recommendation-test.com/posts/1"));
        post2 = postRepository.save(post(testBlog, "게시글 2", "https://recommendation-test.com/posts/2"));
        post3 = postRepository.save(post(testBlog, "게시글 3", "https://recommendation-test.com/posts/3"));
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
        RecommendedPost rank3 = recommendedPost(testUser, post3, 3);
        RecommendedPost rank1 = recommendedPost(testUser, post1, 1);
        RecommendedPost rank2 = recommendedPost(testUser, post2, 2);
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
        User anotherUser = userRepository.save(user(
                "recommendation-repository-another-user",
                "another-recommendation@example.com"
        ));
        recommendedPostRepository.saveAll(List.of(
                recommendedPost(testUser, post1, 1),
                recommendedPost(testUser, post2, 2),
                recommendedPost(anotherUser, post1, 0.7, 0.65, 1)
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
        RecommendedPost firstRecommendation = recommendedPost(testUser, post1, 1);
        RecommendedPost duplicateRecommendation = recommendedPost(testUser, post1, 2);

        recommendedPostRepository.saveAndFlush(firstRecommendation);

        assertThatThrownBy(() -> recommendedPostRepository.saveAndFlush(duplicateRecommendation))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();
    }
}
