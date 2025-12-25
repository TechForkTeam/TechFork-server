package com.techfork.domain.post.service;

import com.techfork.domain.post.converter.PostConverter;
import com.techfork.domain.post.dto.PostResponseDto;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.enums.EPostSortType;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostKeywordRepository postKeywordRepository;
    private final PostConverter postConverter;

    public PostResponseDto.CompanyList getCompanies() {
        List<String> companies = postRepository.findDistinctCompanies();
        return postConverter.toCompanyListResponse(companies);
    }

    public PostResponseDto.PostList getPostsByCompany(String company, Long lastPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostResponseDto.Info> posts = postRepository.findByCompanyWithCursor(company, lastPostId, pageRequest);
        List<PostResponseDto.Info> postsWithKeywords = attachKeywordsToPostInfoList(posts);
        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostResponseDto.PostList getRecentPosts(EPostSortType sortBy, Long lastPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostResponseDto.Info> posts;

        if (sortBy == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursor(lastPostId, pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursor(lastPostId, pageRequest);
        }

        List<PostResponseDto.Info> postsWithKeywords = attachKeywordsToPostInfoList(posts);
        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostResponseDto.Detail getPostDetail(Long postId) {
        PostResponseDto.Detail postDetail = postRepository.findByIdWithTechBlog(postId)
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        List<String> keywords = postKeywordRepository.findByPostIdIn(List.of(postId))
                .stream()
                .map(PostKeyword::getKeyword)
                .toList();

        return postConverter.toPostDetailDto(postDetail, keywords);
    }

    private List<PostResponseDto.Info> attachKeywordsToPostInfoList(List<PostResponseDto.Info> posts) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostResponseDto.Info::id)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

        return posts.stream()
                .map(post -> PostResponseDto.Info.builder()
                        .id(post.id())
                        .title(post.title())
                        .company(post.company())
                        .url(post.url())
                        .logoUrl(post.logoUrl())
                        .publishedAt(post.publishedAt())
                        .viewCount(post.viewCount())
                        .keywords(keywordMap.getOrDefault(post.id(), List.of()))
                        .build())
                .toList();
    }
}
