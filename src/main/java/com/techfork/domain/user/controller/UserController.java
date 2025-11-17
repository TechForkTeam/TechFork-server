package com.techfork.domain.user.controller;

import com.techfork.domain.user.dto.SaveInterestRequest;
import com.techfork.domain.user.service.InterestCommandService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final InterestCommandService interestCommandService;

    @Operation(
            summary = "내 관심사 수정",
            description = "현재 로그인한 사용자의 관심사를 수정합니다. 기존 관심사는 모두 삭제되고 새로운 관심사로 대체됩니다."
    )
    @PutMapping("/me/interests")
    public ResponseEntity<BaseResponse<Void>> updateMyInterests(
            @Valid @RequestBody SaveInterestRequest request
    ) {
        // TODO: userId Auth 인증 기반으로 추출
        Long userId = 1L;

        interestCommandService.updateUserInterests(userId, request);
        return BaseResponse.of(SuccessCode.OK);
    }
}
