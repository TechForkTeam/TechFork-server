package com.techfork.activity.readhistory.controller;

import com.techfork.activity.readhistory.dto.SearchHistoryRequest;
import com.techfork.activity.readhistory.service.ReadHistoryCommandService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity", description = "사용자 활동 API")
@RestController
@RequestMapping("/api/v1/activities/searches")
@RequiredArgsConstructor
public class ReadHistoryController {

    private final ReadHistoryCommandService readHistoryCommandService;

    @Operation(
            summary = "검색 히스토리 저장",
            description = "사용자의 검색어 기록을 저장합니다. 검색어와 검색 시간을 기록합니다."
    )
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> saveSearchHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SearchHistoryRequest request
    ) {
        readHistoryCommandService.saveSearchHistory(userPrincipal.getId(), request);
        return BaseResponse.of(SuccessCode.CREATED);
    }
}
