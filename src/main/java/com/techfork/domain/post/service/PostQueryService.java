package com.techfork.domain.post.service;

import com.techfork.domain.post.converter.PostConverter;
import com.techfork.domain.post.dto.*;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostConverter postConverter;

    public CompanyListResponse getCompanies() {
        List<String> companies = postRepository.findDistinctCompanies();
        return postConverter.toCompanyListResponse(companies);
    }

    public PostListResponse getPostsByCompany(String company, Long lastPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> postDtos = postRepository.findByCompanyWithCursor(company, lastPostId, pageRequest);

        return postConverter.toPostListResponse(postDtos, size);
    }

    public PostListResponse getRecentPosts(PostSortType sortBy, Long lastPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> postDtos;

        if (sortBy == PostSortType.POPULAR) {
            postDtos = postRepository.findPopularPostsWithCursor(lastPostId, pageRequest);
        } else {
            postDtos = postRepository.findRecentPostsWithCursor(lastPostId, pageRequest);
        }

        return postConverter.toPostListResponse(postDtos, size);
    }

    public PostDetailDto getPostDetail(Long postId) {
        Post post = postRepository.findByIdWithTechBlog(postId)
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        return postConverter.toDetailDto(post);
    }
}
