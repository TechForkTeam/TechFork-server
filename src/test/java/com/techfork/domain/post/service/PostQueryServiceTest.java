package com.techfork.domain.post.service;

import com.techfork.domain.post.converter.PostConverter;
import com.techfork.domain.post.dto.*;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.enums.EPostSortType;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * PostQueryService 단위 테스트
 * - Repository와 Converter를 Mock으로 대체
 * - 비즈니스 로직만 검증
 */
@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostKeywordRepository postKeywordRepository;

    @Mock
    private PostConverter postConverter;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("getCompanies() - 회사 목록 조회 성공")
    void getCompanies_Success() {
        // Given
        List<String> mockCompanies = List.of("카카오", "네이버", "라인");
        CompanyListResponse expectedResponse = new CompanyListResponse(3, mockCompanies);

        given(postRepository.findDistinctCompanies()).willReturn(mockCompanies);
        given(postConverter.toCompanyListResponse(mockCompanies)).willReturn(expectedResponse);

        // When
        CompanyListResponse result = postQueryService.getCompanies();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.companies()).hasSize(3);

        @SuppressWarnings("unchecked")
        List<String> companies = (List<String>) result.companies();
        assertThat(companies).contains("카카오", "네이버", "라인");

        verify(postRepository, times(1)).findDistinctCompanies();
        verify(postConverter, times(1)).toCompanyListResponse(mockCompanies);
    }

    @Test
    @DisplayName("getCompaniesV2() - 회사 상세 정보 포함 목록 조회 성공")
    void getCompaniesV2_Success() {
        // Given
        List<CompanyDto> mockCompanies = List.of(
                CompanyDto.builder()
                        .company("카카오")
                        .hasNewPost(true)
                        .logoUrl("https://test.com/kakao-logo.png")
                        .build(),
                CompanyDto.builder()
                        .company("네이버")
                        .hasNewPost(false)
                        .logoUrl("https://test.com/naver-logo.png")
                        .build()
        );

        CompanyListResponse expectedResponse = CompanyListResponse.builder()
                .totalNumber(2)
                .companies(mockCompanies)
                .build();

        given(postRepository.findCompaniesWithDetails()).willReturn(mockCompanies);
        given(postConverter.toCompanyListResponseV2(mockCompanies)).willReturn(expectedResponse);

        // When
        CompanyListResponse result = postQueryService.getCompaniesV2();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.companies()).hasSize(2);

        List<CompanyDto> resultCompanies = (List<CompanyDto>) result.companies();
        assertThat(resultCompanies.get(0).company()).isEqualTo("카카오");
        assertThat(resultCompanies.get(0).hasNewPost()).isTrue();
        assertThat(resultCompanies.get(0).logoUrl()).isEqualTo("https://test.com/kakao-logo.png");
        assertThat(resultCompanies.get(1).company()).isEqualTo("네이버");
        assertThat(resultCompanies.get(1).hasNewPost()).isFalse();

        verify(postRepository, times(1)).findCompaniesWithDetails();
        verify(postConverter, times(1)).toCompanyListResponseV2(mockCompanies);
    }

    @Test
    @DisplayName("getPostDetail() - 게시글 상세 조회 성공")
    void getPostDetail_Success() {
        // Given
        Long postId = 1L;

        PostDetailDto mockPostDetail = PostDetailDto.builder()
                .id(postId)
                .title("테스트 제목")
                .summary("테스트 요약")
                .company("카카오")
                .url("https://test.com/1")
                .logoUrl("https://test.com/logo.png")
                .publishedAt(LocalDateTime.now())
                .viewCount(100L)
                .keywords(null) // 키워드는 나중에 추가됨
                .build();

        PostKeyword keyword1 = mock(PostKeyword.class);
        when(keyword1.getKeyword()).thenReturn("Java");
        PostKeyword keyword2 = mock(PostKeyword.class);
        when(keyword2.getKeyword()).thenReturn("Spring");

        List<PostKeyword> mockKeywords = List.of(keyword1, keyword2);
        List<String> keywordStrings = List.of("Java", "Spring");

        PostDetailDto expectedResponse = PostDetailDto.builder()
                .id(postId)
                .title("테스트 제목")
                .summary("테스트 요약")
                .company("카카오")
                .url("https://test.com/1")
                .logoUrl("https://test.com/logo.png")
                .publishedAt(mockPostDetail.publishedAt())
                .viewCount(100L)
                .keywords(keywordStrings)
                .build();

        given(postRepository.findByIdWithTechBlog(postId)).willReturn(Optional.of(mockPostDetail));
        given(postKeywordRepository.findByPostIdIn(List.of(postId))).willReturn(mockKeywords);
        given(postConverter.toPostDetailDto(mockPostDetail, keywordStrings)).willReturn(expectedResponse);

        // When
        PostDetailDto result = postQueryService.getPostDetail(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(postId);
        assertThat(result.title()).isEqualTo("테스트 제목");
        assertThat(result.viewCount()).isEqualTo(100L);
        assertThat(result.keywords()).hasSize(2);
        assertThat(result.keywords()).contains("Java", "Spring");

        verify(postRepository, times(1)).findByIdWithTechBlog(postId);
        verify(postKeywordRepository, times(1)).findByPostIdIn(List.of(postId));
        verify(postConverter, times(1)).toPostDetailDto(mockPostDetail, keywordStrings);
    }

    @Test
    @DisplayName("getPostDetail() - 존재하지 않는 게시글 조회 시 예외 발생")
    void getPostDetail_NotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        given(postRepository.findByIdWithTechBlog(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPostDetail(postId))
                .isInstanceOf(GeneralException.class);

        verify(postRepository, times(1)).findByIdWithTechBlog(postId);
        verify(postKeywordRepository, never()).findByPostIdIn(any());
    }

    @Test
    @DisplayName("getRecentPosts() - LATEST 정렬로 최근 게시글 조회")
    void getRecentPosts_Latest_Success() {
        // Given
        EPostSortType sortBy = EPostSortType.LATEST;
        Long lastPostId = null;
        int size = 20;

        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(2L)
                        .title("게시글 2")
                        .company("카카오")
                        .url("https://test.com/2")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now())
                        .viewCount(50L)
                        .keywords(null)
                        .build(),
                PostInfoDto.builder()
                        .id(1L)
                        .title("게시글 1")
                        .company("네이버")
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now().minusDays(1))
                        .viewCount(100L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(1L)
                .hasNext(false)
                .build();

        given(postRepository.findRecentPostsWithCursor(eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getRecentPosts(sortBy, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(2);
        assertThat(result.hasNext()).isFalse();

        verify(postRepository, times(1)).findRecentPostsWithCursor(eq(lastPostId), any(PageRequest.class));
        verify(postRepository, never()).findPopularPostsWithCursor(any(), any());
    }

    @Test
    @DisplayName("getRecentPosts() - POPULAR 정렬로 인기 게시글 조회")
    void getRecentPosts_Popular_Success() {
        // Given
        EPostSortType sortBy = EPostSortType.POPULAR;
        Long lastPostId = null;
        int size = 20;

        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(1L)
                        .title("인기 게시글 1")
                        .company("카카오")
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now())
                        .viewCount(1000L)
                        .keywords(null)
                        .build(),
                PostInfoDto.builder()
                        .id(2L)
                        .title("인기 게시글 2")
                        .company("네이버")
                        .url("https://test.com/2")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now())
                        .viewCount(500L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(2L)
                .hasNext(false)
                .build();

        given(postRepository.findPopularPostsWithCursor(eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getRecentPosts(sortBy, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(2);
        assertThat(result.posts().get(0).viewCount()).isGreaterThan(result.posts().get(1).viewCount());

        verify(postRepository, times(1)).findPopularPostsWithCursor(eq(lastPostId), any(PageRequest.class));
        verify(postRepository, never()).findRecentPostsWithCursor(any(), any());
    }

    @Test
    @DisplayName("getPostsByCompany() - 특정 회사의 게시글 조회")
    void getPostsByCompany_Success() {
        // Given
        String company = "카카오";
        Long lastPostId = null;
        int size = 20;

        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(1L)
                        .title("카카오 게시글")
                        .company(company)
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now())
                        .viewCount(50L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(1L)
                .hasNext(false)
                .build();

        given(postRepository.findByCompanyWithCursor(eq(company), eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getPostsByCompany(company, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).company()).isEqualTo(company);

        verify(postRepository, times(1)).findByCompanyWithCursor(eq(company), eq(lastPostId), any(PageRequest.class));
    }

    @Test
    @DisplayName("getPostsByCompanyV2() - 여러 회사의 게시글 조회 성공")
    void getPostsByCompanyV2_Success() {
        // Given
        List<String> companies = List.of("카카오", "네이버");
        LocalDateTime lastPublishedAt = null;
        Long lastPostId = null;
        int size = 20;

        LocalDateTime now = LocalDateTime.now();
        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(2L)
                        .title("네이버 게시글")
                        .company("네이버")
                        .url("https://test.com/2")
                        .logoUrl("https://test.com/naver-logo.png")
                        .publishedAt(now)
                        .viewCount(100L)
                        .keywords(null)
                        .build(),
                PostInfoDto.builder()
                        .id(1L)
                        .title("카카오 게시글")
                        .company("카카오")
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/kakao-logo.png")
                        .publishedAt(now.minusHours(1))
                        .viewCount(50L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(1L)
                .lastPublishedAt(now.minusHours(1))
                .hasNext(false)
                .build();

        given(postRepository.findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getPostsByCompanyV2(companies, lastPublishedAt, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(2);
        assertThat(result.posts().get(0).company()).isEqualTo("네이버");
        assertThat(result.posts().get(1).company()).isEqualTo("카카오");
        assertThat(result.posts().get(0).publishedAt()).isAfter(result.posts().get(1).publishedAt());

        verify(postRepository, times(1)).findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class));
        verify(postKeywordRepository, times(1)).findByPostIdIn(any());
        verify(postConverter, times(1)).toPostListResponse(any(), eq(size));
    }

    @Test
    @DisplayName("getPostsByCompanyV2() - companies가 null이면 전체 게시글 조회")
    void getPostsByCompanyV2_NullCompanies_ReturnsAll() {
        // Given
        List<String> companies = null;
        LocalDateTime lastPublishedAt = null;
        Long lastPostId = null;
        int size = 20;

        LocalDateTime now = LocalDateTime.now();
        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(3L)
                        .title("라인 게시글")
                        .company("라인")
                        .url("https://test.com/3")
                        .logoUrl("https://test.com/line-logo.png")
                        .publishedAt(now)
                        .viewCount(200L)
                        .keywords(null)
                        .build(),
                PostInfoDto.builder()
                        .id(2L)
                        .title("네이버 게시글")
                        .company("네이버")
                        .url("https://test.com/2")
                        .logoUrl("https://test.com/naver-logo.png")
                        .publishedAt(now.minusHours(1))
                        .viewCount(100L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(2L)
                .lastPublishedAt(now.minusHours(1))
                .hasNext(false)
                .build();

        given(postRepository.findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getPostsByCompanyV2(companies, lastPublishedAt, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(2);

        verify(postRepository, times(1)).findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class));
    }

    @Test
    @DisplayName("getPostsByCompanyV2() - 커서 페이징으로 다음 페이지 조회")
    void getPostsByCompanyV2_WithCursor_ReturnsNextPage() {
        // Given
        List<String> companies = List.of("카카오");
        LocalDateTime lastPublishedAt = LocalDateTime.now().minusHours(2);
        Long lastPostId = 100L;
        int size = 20;

        List<PostInfoDto> mockPosts = List.of(
                PostInfoDto.builder()
                        .id(99L)
                        .title("카카오 게시글 99")
                        .company("카카오")
                        .url("https://test.com/99")
                        .logoUrl("https://test.com/kakao-logo.png")
                        .publishedAt(lastPublishedAt.minusMinutes(10))
                        .viewCount(50L)
                        .keywords(null)
                        .build()
        );

        PostListResponse expectedResponse = PostListResponse.builder()
                .posts(mockPosts)
                .lastPostId(99L)
                .lastPublishedAt(lastPublishedAt.minusMinutes(10))
                .hasNext(false)
                .build();

        given(postRepository.findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class)))
                .willReturn(mockPosts);
        given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
        given(postConverter.toPostListResponse(any(), eq(size))).willReturn(expectedResponse);

        // When
        PostListResponse result = postQueryService.getPostsByCompanyV2(companies, lastPublishedAt, lastPostId, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).id()).isEqualTo(99L);

        verify(postRepository, times(1)).findByCompanyNamesWithCursor(eq(companies), eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class));
    }
}
