package com.techfork.domain.recommendation.fixture;

import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;

public final class RecommendedPostFixture {

    private RecommendedPostFixture() {
    }

    public static RecommendedPost recommendedPost(User user, Post post, int rankOrder) {
        double similarityScore = 1.0 - (rankOrder * 0.1);
        double mmrScore = 0.95 - (rankOrder * 0.1);
        return recommendedPost(user, post, similarityScore, mmrScore, rankOrder);
    }

    public static RecommendedPost recommendedPost(
            User user,
            Post post,
            double similarityScore,
            double mmrScore,
            int rankOrder
    ) {
        return RecommendedPost.create(user, post, similarityScore, mmrScore, rankOrder);
    }
}
