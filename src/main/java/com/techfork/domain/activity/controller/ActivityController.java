package com.techfork.domain.activity.controller;

import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.dto.BookmarkRequest;
import com.techfork.domain.activity.dto.ReadPostListResponse;
import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.service.ActivityCommandService;
import com.techfork.domain.activity.service.ActivityQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity", description = "사용자 활동 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityCommandService activityCommandService;
    private final ActivityQueryService activityQueryService;
    
    @Operation(
            summary = "읽은 게시글 목록 조회",
            description = "사용자가 읽은 게시글 목록을 조회합니다. 최근 읽은 순서로 정렬됩니다."
    )
    @GetMapping("/read-posts")
    public ResponseEntity<BaseResponse<ReadPostListResponse>> getReadPosts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "마지막 읽은 게시글 ID (커서, 선택)")
            @RequestParam(required = false) Long lastReadPostId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        ReadPostListResponse response = activityQueryService.getReadPosts(userPrincipal.getId(), lastReadPostId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }
    
    @Operation(
            summary = "읽은 게시글 저장",
            description = "사용자가 특정 게시글을 읽은 기록을 저장합니다. 읽은 시간과 체류 시간을 기록합니다."
    )
    @PostMapping("/read-posts")
    public ResponseEntity<BaseResponse<Void>> saveReadPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReadPostRequest request
    ) {
        activityCommandService.saveReadPost(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "검색 히스토리 저장",
            description = "사용자의 검색어 기록을 저장합니다. 검색어와 검색 시간을 기록합니다."
    )
    @PostMapping("/searches")
    public ResponseEntity<BaseResponse<Void>> saveSearchHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SearchHistoryRequest request
    ) {
        activityCommandService.saveSearchHistory(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "북마크 목록 조회",
            description = "사용자가 북마크한 게시글 목록을 조회합니다. 최근 북마크한 순서로 정렬됩니다."
    )
    @GetMapping("/bookmarks")
    public ResponseEntity<BaseResponse<BookmarkListResponse>> getBookmarks(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "마지막 북마크 ID (커서, 선택)")
            @RequestParam(required = false) Long lastBookmarkId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        BookmarkListResponse response = activityQueryService.getBookmarks(userPrincipal.getId(), lastBookmarkId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "북마크 추가",
            description = "특정 게시글을 북마크에 추가합니다. 중복 방지 처리가 적용됩니다."
    )
    @PostMapping("/bookmarks")
    public ResponseEntity<BaseResponse<Void>> addBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BookmarkRequest request
    ) {
        activityCommandService.addBookmark(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "북마크 삭제",
            description = "특정 게시글의 북마크를 제거합니다."
    )
    @DeleteMapping("/bookmarks")
    public ResponseEntity<BaseResponse<Void>> deleteBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BookmarkRequest request
    ) {
        activityCommandService.deleteBookmark(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.OK);
    }
}
