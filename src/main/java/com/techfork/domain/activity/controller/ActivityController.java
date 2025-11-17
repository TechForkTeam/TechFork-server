package com.techfork.domain.activity.controller;

import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.service.ActivityCommandService;
import com.techfork.domain.activity.service.ActivityQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            summary = "읽은 게시글 저장",
            description = "사용자가 특정 게시글을 읽은 기록을 저장합니다. 읽은 시간과 체류 시간을 기록합니다."
    )
    @PostMapping("/read-posts")
    public ResponseEntity<BaseResponse<Void>> saveReadPost(
            @Valid @RequestBody ReadPostRequest request
    ) {

        // TODO: userId Auth 인증 기반으로 추출
        Long userId = 1L;

        activityCommandService.saveReadPost(userId, request);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "검색 히스토리 저장",
            description = "사용자의 검색어 기록을 저장합니다. 검색어와 검색 시간을 기록합니다."
    )
    @PostMapping("/searches")
    public ResponseEntity<BaseResponse<Void>> saveSearchHistory(
            @Valid @RequestBody SearchHistoryRequest request
    ) {

        // TODO: userId Auth 인증 기반으로 추출
        Long userId = 1L;

        activityCommandService.saveSearchHistory(userId, request);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "북마크 목록 조회",
            description = "사용자가 북마크한 게시글 목록을 조회합니다. 최근 북마크한 순서로 정렬됩니다."
    )
    @GetMapping("/bookmarks")
    public ResponseEntity<BaseResponse<BookmarkListResponse>> getBookmarks(
            @Parameter(description = "마지막 북마크 ID (커서, 선택)")
            @RequestParam(required = false) Long lastBookmarkId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {

        // TODO: userId Auth 인증 기반으로 추출
        Long userId = 1L;

        BookmarkListResponse response = activityQueryService.getBookmarks(userId, lastBookmarkId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
