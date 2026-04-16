# Search

> 일반 검색과 개인화 검색을 수행하는 query service / read model 중심 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/search`
- 관련 read model: `src/main/java/com/techfork/domain/post/document`, `src/main/java/com/techfork/domain/user/document`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 검색어 / SearchQuery | `query`, 현재 `searchWord` | 사용자가 찾고 싶은 기술 주제/키워드 |
| BM25 검색 / 어휘 검색 | `searchOnlyBm25`, `performLexicalSearch` | 제목/요약/본문 청크 텍스트 기반 검색 |
| 시맨틱 검색 | `searchOnlySemantic`, `performSemanticSearch` | 검색어 임베딩과 기술 게시글 임베딩의 k-NN 기반 검색 |
| 하이브리드 검색 | `performHybridSearch` | BM25 검색과 시맨틱 검색을 병렬 실행한 뒤 결합하는 검색 |
| RRF | `RrfScorer` | BM25 결과와 시맨틱 결과 순위를 결합하는 Reciprocal Rank Fusion |
| 일반 검색 | `searchGeneral` | 사용자 프로필 없이 하이브리드 검색 결과를 반환하는 검색 |
| 개인화 검색 | `searchPersonalized` | 사용자 프로필 벡터가 있으면 검색 결과를 개인화 리랭킹하는 검색 |
| 개인화 리랭킹 | `personalReranking` | 사용자 프로필 벡터와 기술 게시글 제목/요약 벡터의 유사도를 반영해 재정렬하는 과정 |
| 검색 결과 | `SearchResult` | 검색 응답 단위 |
| 하이브리드 점수 | `hybridScore` | RRF로 결합된 검색 점수 |
| 개인화 점수 | `personalScore` | 사용자 프로필과 검색 결과 문서 간 유사도 점수 |
| 최종 점수 | `finalScore` | 하이브리드 점수와 개인화 점수를 조합한 최종 정렬 점수 |
| 프로필 없음 fallback | `Personalized Search [FALLBACK]` | 사용자 프로필이 없을 때 일반 하이브리드 결과를 반환하는 흐름 |

## 경계 메모

- Search는 **aggregate 중심이 아니라 query/read model 중심** 컨텍스트다.
- `SearchQuery`는 사용자가 입력한 문자열만 가리킨다. `keyKeywords`, `PostKeyword`와 혼용하지 않는다.
- 북마크 여부 조합은 조회 조합 책임으로 본다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 어휘 검색 | `performLexicalSearch` | BM25 계열 텍스트 검색 |
| 시맨틱 검색 | `performSemanticSearch` | 임베딩 기반 k-NN 검색 |
| 하이브리드 결합 | `performHybridSearch` | 어휘 검색과 시맨틱 검색을 병렬 실행하고 합치는 흐름 |
| 리랭킹 | `personalReranking` | 사용자 프로필 벡터를 반영한 재정렬 |
| fallback 검색 | `Personalized Search [FALLBACK]` | 개인화 프로필이 없을 때 일반 검색으로 내려가는 흐름 |
| 메타데이터 부착 | `attachPostMetadata` | 조회수, 북마크 여부 등 응답 조합용 후처리 |

## 혼동 금지

- `검색 결과`는 저장되는 aggregate가 아니라 계산된 응답이다.
- Search가 `PostDocument`, `UserProfileDocument`를 읽는다고 해서 Post/User aggregate를 소유하는 것은 아니다.
- `검색어(SearchQuery)`와 추천의 `keyKeywords`는 둘 다 문자열이지만 생성 주체가 다르다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 키워드 검색 | BM25 검색 / 어휘 검색 | 어떤 검색 전략인지 더 정확히 드러낸다 |
| 벡터 검색 | 시맨틱 검색 | 사용자/문서 임베딩 기반 의미 검색임을 드러낸다 |
| 프로필 검색 | 개인화 검색 | 사용자 프로필 반영 여부를 명확히 표현한다 |
| 검색어 키워드 | 검색어 / SearchQuery | 입력 문자열과 추천용 키워드를 섞지 않기 위해 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/search/service/SearchService.java`
- `src/main/java/com/techfork/domain/search/service/SearchServiceImpl.java`
- `src/main/java/com/techfork/domain/search/dto/SearchResult.java`
- `src/main/java/com/techfork/domain/search/config/GeneralSearchProperties.java`
