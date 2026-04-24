# Activity

> 사용자의 읽기/검색/북마크 행동 기록을 다루는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/activity`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 읽은 게시글 | `ReadPost` | 사용자가 기술 게시글을 읽은 기록 |
| 읽은 시간 | `readDurationSeconds` | 사용자가 기술 게시글을 읽은 지속 시간 |
| 읽기 몰입도 | `convertReadingDurationToNaturalLanguage` | 읽은 시간을 `가볍게 훑어봄`, `빠르게 읽음`, `읽음`, `정독함`, `깊게 읽음`으로 해석한 값 |
| 첫 읽기 | `isFirstRead` | 특정 기술 게시글을 처음 읽은 경우 |
| 검색 기록 | `SearchHistory` | 사용자가 입력한 검색어와 검색 시각 |
| 검색어 | `query` (legacy alias: `searchWord`) | 검색 기록에 저장되는 사용자 입력 |
| 북마크 | `Bookmark`, 현재 `ScrabPost` | 사용자가 기술 게시글을 저장하는 행위 |
| 북마크 여부 | `isBookmarked` | 목록/검색/추천 응답에 붙는 사용자별 저장 여부 |

## 용어 정합성 결정

- 제품/API/문서 언어 기준으로 **북마크**로 통일한다.
- 레거시 이름 `ScrabPost`, `scrap_posts`는 코드/DB 잔존 용어로만 취급한다.
- `SearchHistory`의 코드/DB 필드명은 `query`로 맞춘다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 읽기 기록 | `ReadPost` | 사용자와 기술 게시글의 읽기 이벤트 레코드 |
| 최초 읽기 판별 | `isFirstRead` | 조회수 증가 여부를 결정하는 플래그 |
| 북마크 레코드 | `ScrabPost` | 현재 코드상 북마크 저장 레코드의 legacy 이름 |
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

## 주요 근거 파일

- `src/main/java/com/techfork/domain/activity/entity/ReadPost.java`
- `src/main/java/com/techfork/domain/activity/entity/SearchHistory.java`
- `src/main/java/com/techfork/domain/activity/entity/ScrabPost.java`
- `src/main/java/com/techfork/domain/activity/service/ActivityCommandService.java`
- `src/main/java/com/techfork/domain/activity/service/ActivityQueryService.java`
