# Post / Content

> TechFork의 핵심 콘텐츠인 기술 게시글과 그 요약/키워드/검색 projection을 다루는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/post`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 기술 게시글 | `Post` | TechFork가 다루는 핵심 콘텐츠. 외부 기술 블로그 RSS 피드 아이템에서 생성된다. |
| 제목 | `title` | 기술 게시글 제목. 검색/임베딩의 주요 대상 |
| 원문 본문 | `fullContent` | RSS에서 가져온 HTML/마크다운 포함 본문 |
| 정제 본문 | `plainContent` | HTML/마크다운을 제거한 검색/요약용 텍스트 |
| 요약 | `summary` | LLM이 생성한 상세 요약 |
| 짧은 요약 | `shortSummary` | 목록/카드 UI에 적합한 축약 요약 |
| 게시글 키워드 | `PostKeyword` | LLM 요약 과정에서 추출된 기술 게시글 대표 키워드 |
| 발행일 | `publishedAt` | 외부 기술 블로그에서 게시글이 발행된 시각 |
| 수집일 | `crawledAt` | TechFork가 해당 기술 게시글을 수집한 시각 |
| 임베딩 완료일 | `embeddedAt` | 기술 게시글이 임베딩되어 Elasticsearch에 색인된 시각 |
| 조회수 | `viewCount` | 사용자가 처음 읽은 경우 증가하는 popularity 지표 |
| 조회수 증가 경로 | `PostViewCountCommandService.incrementViewCount()` | production에서 조회수를 증가시키는 canonical command 경로 |
| 검색 문서 | `PostDocument` | Elasticsearch `posts` 인덱스에 저장되는 기술 게시글 projection |
| 콘텐츠 청크 | `ContentChunk` | 긴 본문을 임베딩 검색용으로 분할한 단위 |
| 출처명 | `Post.company`, `TechBlog.companyName` | 기술 게시글이 어느 기술 블로그/회사에서 왔는지 표시하기 위한 이름 |
| 정렬 기준 | `EPostSortType` | `LATEST` 최신순, `POPULAR` 인기순 |

## 경계 메모

- 도메인/기획 문서에서는 `Post`를 **기술 게시글**로 부른다.
- `PostDocument`, `ContentChunk`는 aggregate가 아니라 **검색/추천용 projection**이다.
- `Post.company`는 Source 컨텍스트의 출처명을 복사한 조회용 스냅샷이다.
- Activity 컨텍스트가 게시글/키워드를 읽을 때의 현재 진입점은 `PostLookupService`, `PostKeywordLookupService`다. 다만 이것은 published query 라기보다 repository 직접 의존을 막는 임시 application seam 에 가깝다.
- Search 는 후보 탐색에 `PostDocument` 를 사용하고, `viewCount` 같은 응답 metadata 는 별도 query composition 으로 읽는다.
- `Post.create(RssFeedItem, TechBlog)`는 현재 Source 컨텍스트가 정제한 **monolith 내부 handoff DTO** 를 받는 생성 경계로 유지한다.
- `RssFeedItem` 직접 참조를 없애는 별도 published language, command object, 이벤트 handoff 는 후속 리팩토링 후보로 남긴다.
- production 경로에서는 `Post.incrementViewCount()` 같은 엔티티 필드 증가를 사용하지 않고 `PostViewCountCommandService`/`PostRepository`의 SQL atomic update를 canonical write path로 둔다.
- Activity 컨텍스트에서는 `first_read_posts(user_id, post_id)` dedupe ledger를 통과한 최초 읽기에서만 `PostViewCountCommandService.incrementViewCount()`를 호출한다.
- `PostViewCountCommandService.incrementViewCount()`는 DB 값을 원자적으로 증가시키지만, 이미 로드된 managed `Post`의 `viewCount`를 같은 트랜잭션 안에서 최신 상태로 동기화하지는 않는다.
- `EDifficultyLevel`은 실제 사용처가 없어 제거되었다. 필요해지면 정책과 함께 재도입한다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 게시글 루트 | `Post` | RDB 기준 핵심 aggregate root |
| 게시글 키워드 컬렉션 | `PostKeyword` | `Post`에 종속된 키워드 컬렉션 |
| 검색 projection | `PostDocument` | Elasticsearch에 저장되는 게시글 읽기 모델 |
| 청크 projection | `ContentChunk` | 긴 본문을 분할한 임베딩 검색 단위 |
| 임베딩 완료 시각 | `embeddedAt` | 검색/추천용 색인 준비 완료 시각 |
| 인기 지표 | `viewCount` | 조회수 기반 popularity 지표 |
| 조회수 증가 command | `PostViewCountCommandService.incrementViewCount` | 조회수 증가를 DB atomic update로 위임하는 application command |

## 혼동 금지

- `기술 게시글`과 `검색 문서(PostDocument)`를 같은 것으로 취급하지 않는다.
- `출처명`은 `TechBlog`의 진실 원천을 복사한 값이지, Post 컨텍스트 자체의 독립 모델이 아니다.
- `게시글 키워드(PostKeyword)`와 `핵심 키워드(KeyKeyword)`는 다르다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 포스트 | 기술 게시글 | 제품/도메인 의미를 더 분명하게 드러낸다 |
| 문서 | 검색 문서 / 게시글 projection | `Post` aggregate와 `PostDocument`를 구분해야 한다 |
| 회사명 | 출처명 | Post에서는 출처 표시를 위한 스냅샷 값이라는 점을 드러내야 한다 |
| 키워드 | 게시글 키워드 | `keyKeywords`와 혼동하지 않기 위해 |

## 주요 근거 파일

- `src/main/java/com/techfork/post/domain/Post.java`
- `src/main/java/com/techfork/post/domain/PostKeyword.java`
- `src/main/java/com/techfork/post/application/command/PostViewCountCommandService.java`
- `src/main/java/com/techfork/post/infrastructure/PostRepository.java`
- `src/main/java/com/techfork/post/domain/projection/PostDocument.java`
- `src/main/java/com/techfork/post/domain/projection/ContentChunk.java`
- `src/main/java/com/techfork/post/application/batch/PostSummaryProcessor.java`
- `src/main/java/com/techfork/post/application/batch/PostEmbeddingProcessor.java`
