package com.techfork.useraccount.presentation;

import com.techfork.useraccount.application.command.InterestCommandService;
import com.techfork.useraccount.application.command.UserCommandService;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.application.query.GetAccountProfileQuery;
import com.techfork.useraccount.application.query.GetUserInterestsQuery;
import com.techfork.useraccount.application.query.InterestQueryService;
import com.techfork.useraccount.application.query.UserQueryService;
import com.techfork.useraccount.application.query.result.GetAccountProfileResult;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.presentation.request.SaveInterestRequest;
import com.techfork.useraccount.presentation.request.UpdateAccountProfileRequest;
import com.techfork.useraccount.presentation.response.AccountProfileResponse;
import com.techfork.useraccount.presentation.response.UserInterestResponse;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final InterestQueryService interestQueryService;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final InterestConverter interestConverter;
    private final UserConverter userConverter;

    @Operation(
            summary = "내 관심사 수정",
            description = "현재 로그인한 사용자의 관심사를 수정합니다. 기존 관심사는 모두 삭제되고 새로운 관심사로 대체됩니다."
    )
    @PutMapping("/me/interests")
    public ResponseEntity<BaseResponse<Void>> updateMyInterests(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SaveInterestRequest request
    ) {
        interestCommandService.updateUserInterests(
                interestConverter.toUpdateUserInterestsCommand(userPrincipal.getId(), request)
        );
        return BaseResponse.of(SuccessCode.OK);
    }

    @Operation(
            summary = "내 관심사 조회",
            description = "현재 로그인한 사용자의 관심사 목록을 조회합니다."
    )
    @GetMapping("/me/interests")
    public ResponseEntity<BaseResponse<UserInterestResponse>> getMyInterests(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GetUserInterestsResult result = interestQueryService.getUserInterests(new GetUserInterestsQuery(userPrincipal.getId()));
        UserInterestResponse response = interestConverter.toUserInterestResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "내 계정 프로필 수정",
            description = "현재 로그인한 사용자의 계정 프로필 정보를 수정합니다. 닉네임과 자기소개를 선택적으로 수정할 수 있습니다."
    )
    @PatchMapping("/me/profile")
    public ResponseEntity<BaseResponse<Void>> updateMyAccountProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateAccountProfileRequest request
    ) {
        userCommandService.updateAccountProfile(
                userConverter.toUpdateAccountProfileCommand(userPrincipal.getId(), request)
        );
        return BaseResponse.of(SuccessCode.OK);
    }

    @Operation(
            summary = "내 계정 프로필 조회",
            description = "현재 로그인한 사용자의 계정 프로필 정보를 조회합니다. (프로필 이미지, 닉네임, 이메일, 자기소개)"
    )
    @GetMapping("/me/profile")
    public ResponseEntity<BaseResponse<AccountProfileResponse>> getMyAccountProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GetAccountProfileResult result = userQueryService.getAccountProfile(new GetAccountProfileQuery(userPrincipal.getId()));
        AccountProfileResponse response = userConverter.toAccountProfileResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다. 계정 프로필 개인정보는 즉시 익명화되며, 활동 기록은 통계 목적으로 유지됩니다. 탈퇴 후 동일한 소셜 계정으로 재가입 시 새로운 회원으로 온보딩부터 다시 시작합니다."
    )
    @PatchMapping("/me/withdrawal")
    public ResponseEntity<BaseResponse<Void>> withdrawUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        userCommandService.withdrawUser(new WithdrawUserCommand(userPrincipal.getId()));
        return BaseResponse.of(SuccessCode.OK);
    }
}
