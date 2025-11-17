package com.techfork.domain.activity.controller;

import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.service.ActivityCommandService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity", description = "사용자 활동 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityCommandService activityCommandService;

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
}
