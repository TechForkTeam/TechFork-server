package com.techfork.activity.bookmark.presentation;

import com.techfork.activity.bookmark.application.command.AddBookmarkCommand;
import com.techfork.activity.bookmark.application.command.BookmarkCommandService;
import com.techfork.activity.bookmark.application.command.DeleteBookmarkCommand;
import com.techfork.activity.bookmark.application.query.GetBookmarksResult;
import com.techfork.activity.bookmark.application.query.BookmarkQueryService;
import com.techfork.activity.bookmark.application.query.GetBookmarksQuery;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity", description = "사용자 활동 API")
@RestController
@RequestMapping("/api/v1/activities/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkCommandService bookmarkCommandService;
    private final BookmarkQueryService bookmarkQueryService;
    private final BookmarkConverter bookmarkConverter;

    @Operation(
            summary = "북마크 목록 조회",
            description = "사용자가 북마크한 게시글 목록을 조회합니다. 최근 북마크한 순서로 정렬됩니다."
    )
    @GetMapping
    public ResponseEntity<BaseResponse<BookmarkListResponse>> getBookmarks(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "마지막 북마크 ID (커서, 선택)")
            @RequestParam(required = false) Long lastBookmarkId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        GetBookmarksQuery query = new GetBookmarksQuery(userPrincipal.getId(), lastBookmarkId, size);
        GetBookmarksResult result = bookmarkQueryService.getBookmarks(query);
        BookmarkListResponse response = bookmarkConverter.toBookmarkListResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "북마크 추가",
            description = "특정 게시글을 북마크에 추가합니다. 중복 방지 처리가 적용됩니다."
    )
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> addBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BookmarkRequest request
    ) {
        AddBookmarkCommand command = new AddBookmarkCommand(userPrincipal.getId(), request.postId());
        bookmarkCommandService.addBookmark(command);
        return BaseResponse.of(SuccessCode.CREATED);
    }

    @Operation(
            summary = "북마크 삭제",
            description = "특정 게시글의 북마크를 제거합니다."
    )
    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> deleteBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BookmarkRequest request
    ) {
        DeleteBookmarkCommand command = new DeleteBookmarkCommand(userPrincipal.getId(), request.postId());
        bookmarkCommandService.deleteBookmark(command);
        return BaseResponse.of(SuccessCode.OK);
    }
}
