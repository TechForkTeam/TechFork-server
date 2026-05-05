package com.techfork.activity.readpost.controller;

import com.techfork.activity.readpost.dto.ReadPostListResponse;
import com.techfork.activity.readpost.dto.ReadPostRequest;
import com.techfork.activity.readpost.service.ReadPostCommandService;
import com.techfork.activity.readpost.service.ReadPostQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity", description = "사용자 활동 API")
@RestController
@RequestMapping("/api/v1/activities/read-posts")
@RequiredArgsConstructor
public class ReadPostController {

    private final ReadPostCommandService readPostCommandService;
    private final ReadPostQueryService readPostQueryService;

    @Operation(
            summary = "읽은 게시글 목록 조회",
            description = "사용자가 읽은 게시글 목록을 조회합니다. 최근 읽은 순서로 정렬됩니다."
    )
    @GetMapping
    public ResponseEntity<BaseResponse<ReadPostListResponse>> getReadPosts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "마지막 읽은 게시글 ID (커서, 선택)")
            @RequestParam(required = false) Long lastReadPostId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        ReadPostListResponse response = readPostQueryService.getReadPosts(userPrincipal.getId(), lastReadPostId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "읽은 게시글 저장",
            description = "사용자가 특정 게시글을 읽은 기록을 저장합니다. 읽은 시간과 체류 시간을 기록합니다."
    )
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> saveReadPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReadPostRequest request
    ) {
        readPostCommandService.saveReadPost(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.CREATED);
    }
}
