# Activity

> 사용자의 읽기/검색/북마크 행동 기록을 다루는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/activity`
- `src/main/java/com/techfork/activity/bookmark`
- `src/main/java/com/techfork/activity/readhistory`
- `src/main/java/com/techfork/activity/readpost`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 읽은 게시글 | `ReadPost` | 사용자가 기술 게시글을 읽은 기록 |
| 읽은 시간 | `readDurationSeconds` | 사용자가 기술 게시글을 읽은 지속 시간 |
| 읽기 몰입도 | `convertReadingDurationToNaturalLanguage` | 읽은 시간을 `가볍게 훑어봄`, `빠르게 읽음`, `읽음`, `정독함`, `깊게 읽음`으로 해석한 값 |
| 첫 읽기 | `ReadPostFirstReadPolicy.markFirstRead` / `FirstReadPost` | 특정 기술 게시글에 대해 조회수 증가 자격을 처음 획득한 경우 |
| 검색 기록 | `SearchHistory` | 사용자가 입력한 검색어와 검색 시각 |
| 검색어 | `query` (legacy alias: `searchWord`) | 검색 기록에 저장되는 사용자 입력 |
| 북마크 | `Bookmark` (legacy alias: `ScrabPost`) | 사용자가 기술 게시글을 저장하는 행위 |
| 북마크 여부 | `isBookmarked` | 목록/검색/추천 응답에 붙는 사용자별 저장 여부 |

## 용어 정합성 결정

- 제품/API/문서 언어 기준으로 **북마크**로 통일한다.
- 레거시 이름 `ScrabPost`, `scrap_posts`는 과거 코드/DB 또는 migration 문맥에서만 취급한다.
- `SearchHistory`의 코드/DB 필드명은 `query`로 맞춘다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 읽기 기록 | `ReadPost` | 사용자와 기술 게시글의 읽기 이벤트 레코드 |
| 최초 읽기 마킹 | `ReadPostFirstReadPolicy.markFirstRead` | `first_read_posts` 유니크 제약을 이용해 조회수 증가 여부를 결정하는 정책 |
| 조회수 증가 위임 | `PostViewCountCommandService.incrementViewCount` | 첫 읽기일 때 Post 컨텍스트에 조회수 증가를 위임하는 command 경로 |
| 최초 읽기 ledger | `FirstReadPost`, `first_read_posts` | 사용자별 게시글 조회수 dedupe를 담당하는 보조 record |
| 북마크 레코드 | `Bookmark` (legacy name: `ScrabPost`) | 북마크 저장 레코드의 현재 표준 이름과 과거 이름을 함께 설명한다 |
| 검색 기록 레코드 | `SearchHistory` | 사용자 검색어를 시간순으로 남기는 레코드 |
| 북마크 여부 조합값 | `isBookmarked` | Search/Post/Recommendation 응답 조립 시 붙는 파생 값 |

## 혼동 금지

- `검색어`는 Activity에 저장되는 행위 로그이기도 하고 Search의 입력이기도 하다. 두 컨텍스트에서 같은 표준 용어를 쓰되 역할은 구분한다.
- `북마크 여부`는 aggregate 필드가 아니라 조회 응답에 붙는 조합값이다.
- `ReadPost`는 읽기 기록이지 조회수 자체가 아니다. 조회수 증가는 Post 컨텍스트의 popularity 지표다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 스크랩 | 북마크 | 제품/API 언어와 맞추기 위해 |
| searchWord | 검색어 / SearchQuery, 코드에서는 `query` | 문서에는 표준 용어를, 코드에는 canonical name을 사용한다 |
| 읽은 포스트 | 읽은 게시글 | Post 컨텍스트 표준 용어와 맞춘다 |
| bookmarked 필드 | 북마크 여부 | 응답 조합값이라는 의미를 더 분명히 한다 |

## 현재 구조 메모

- 현재 브랜치 기준으로 `Bookmark`, `ReadPost`, `SearchHistory`는 모두 `presentation / application / domain / infrastructure` 기준으로 정리되어 있다.
- `ReadPost` 저장은 `UserLookupService`, `PostLookupService`, `ReadPostFirstReadPolicy`, `PostViewCountCommandService`를 조합해 읽기 이력 저장과 조회수 증가를 분리한다.
- `ReadPostFirstReadPolicy.markFirstRead()`는 `first_read_posts(user_id, post_id)` 유니크 제약을 first-read 판정의 단일 진실 원천으로 사용한다.
- `ReadPostCommandService`는 first-read 마킹이 성공했을 때만 `PostViewCountCommandService.incrementViewCount()`를 호출하고, 조회수 증가 실패 시 예외를 던져 전체 트랜잭션을 롤백한다.
- `ReadPost` 조회는 `bookmark.infrastructure.BookmarkRepository`를 직접 참조하지 않고 `bookmark.application.query.lookup.BookmarkLookupService`를 통해 북마크 여부를 조합한다.
- `ReadPost` 목록 조회 `size`는 HTTP layer에서 `1..100`으로 검증한다.
- `SearchHistory` 저장은 `SearchHistoryRequest -> SaveSearchHistoryCommand -> ReadHistoryCommandService` 흐름을 따른다.
- Activity application 서비스의 cross-context 조회/명령 의존은 `UserLookupService`, `PostLookupService`, `PostKeywordLookupService`, `BookmarkLookupService`, `PostViewCountCommandService`를 통해 application 간 의존으로 정리되어 있다.
- aggregate/value object 강화, hexagonal architecture(포트/어댑터), `ManyToOne -> ID reference` 전환은 후속 정리 범위다.

## 주요 근거 파일

- `src/main/java/com/techfork/activity/readpost/domain/ReadPost.java`
- `src/main/java/com/techfork/activity/readpost/domain/FirstReadPost.java`
- `src/main/java/com/techfork/activity/readpost/domain/ReadPostErrorCode.java`
- `src/main/java/com/techfork/activity/readpost/domain/ReadPostFirstReadPolicy.java`
- `src/main/java/com/techfork/activity/readpost/application/command/ReadPostCommandService.java`
- `src/main/java/com/techfork/activity/readpost/application/command/SaveReadPostCommand.java`
- `src/main/java/com/techfork/activity/readpost/application/query/ReadPostQueryService.java`
- `src/main/java/com/techfork/activity/readpost/application/query/GetReadPostsQuery.java`
- `src/main/java/com/techfork/activity/readpost/infrastructure/FirstReadPostRepository.java`
- `src/main/java/com/techfork/activity/readpost/infrastructure/FirstReadPostRepositoryImpl.java`
- `src/main/java/com/techfork/activity/readpost/presentation/ReadPostConverter.java`
- `src/main/java/com/techfork/activity/readpost/presentation/ReadPostController.java`
- `src/main/java/com/techfork/activity/readhistory/domain/SearchHistory.java`
- `src/main/java/com/techfork/activity/bookmark/domain/Bookmark.java`
- `src/main/java/com/techfork/activity/bookmark/application/command/BookmarkCommandService.java`
- `src/main/java/com/techfork/activity/bookmark/application/query/BookmarkQueryService.java`
- `src/main/java/com/techfork/activity/bookmark/application/query/lookup/BookmarkLookupService.java`
- `src/main/java/com/techfork/activity/bookmark/infrastructure/BookmarkRepository.java`
- `src/main/java/com/techfork/activity/bookmark/presentation/BookmarkConverter.java`
- `src/main/java/com/techfork/activity/bookmark/presentation/BookmarkController.java`
- `src/main/java/com/techfork/useraccount/application/query/lookup/UserLookupService.java`
- `src/main/java/com/techfork/post/application/query/lookup/PostLookupService.java`
- `src/main/java/com/techfork/post/application/command/PostViewCountCommandService.java`
- `src/main/java/com/techfork/post/application/query/lookup/PostKeywordLookupService.java`
- `src/main/java/com/techfork/activity/readhistory/application/command/ReadHistoryCommandService.java`
- `src/main/java/com/techfork/activity/readhistory/application/command/SaveSearchHistoryCommand.java`
- `src/main/java/com/techfork/activity/readhistory/presentation/SearchHistoryRequest.java`
- `src/main/java/com/techfork/activity/readhistory/presentation/ReadHistoryController.java`
