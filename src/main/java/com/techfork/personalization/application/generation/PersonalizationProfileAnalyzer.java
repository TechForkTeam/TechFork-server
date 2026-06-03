package com.techfork.personalization.application.generation;

import com.techfork.global.llm.LlmClient;
import com.techfork.personalization.application.activity.PostActivityData;
import com.techfork.personalization.application.activity.UserActivityData;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationProfileAnalyzer {

    private static final String SYSTEM_PROMPT = "당신은 테크 블로그 플랫폼의 사용자 프로필 분석 전문가입니다. 사용자의 활동 데이터를 분석하여 검색 고도화와 포스트 추천에 최적화된 프로필을 생성합니다.";

    private final LlmClient llmClient;

    public PersonalizationProfileAnalysis analyze(UserActivityData data) {
        String userPrompt = buildProfileGenerationPrompt(data);
        String llmResponse = llmClient.call(SYSTEM_PROMPT, userPrompt);
        return parseProfileAndKeywords(llmResponse);
    }

    private String buildProfileGenerationPrompt(UserActivityData data) {
        return String.format("""
                아래 사용자의 활동 데이터를 분석하여 검색 리랭킹과 포스트 추천에 최적화된 프로필을 생성해주세요.

                ## 사용자 데이터

                ### 관심 기술 스택 및 분야
                %s

                ### 최근 읽은 포스트
                %s

                ### 스크랩한 포스트
                %s

                ### 검색 기록
                %s

                ## 요구사항

                반드시 아래 형식으로 응답해주세요:

                ### PROFILE
                사용자의 기술적 관심사, 학습 패턴, 선호도를 의미 밀도 높고 풍부하게 표현한 텍스트를 작성하세요 (200-300자 정도).

                다음 내용을 모두 포함하되 자연스러운 문장으로 작성:
                1. 주요 관심 기술 스택과 개발 분야 (백엔드/프론트엔드/인프라/AI 등)
                2. 선호하는 주제와 학습 방향 (아키텍처 설계, 성능 최적화, 트러블슈팅, 신기술 탐구 등)
                3. 읽은 포스트와 검색 기록에서 드러나는 구체적인 관심사
                4. 현재 해결하려는 문제나 학습 중인 영역
                5. 콘텐츠 선호 패턴 (심화 기술, 실전 경험, 튜토리얼 등)

                주의사항:
                - 마크다운 없이 순수 텍스트로만 작성 (볼드, 이탤릭, 리스트, 번호 금지)
                - 구체적인 기술 용어를 많이 사용하여 임베딩 품질 향상
                - "관심이 있습니다", "선호합니다" 같은 메타 표현 대신 직접적인 기술 용어 나열

                ### KEYWORDS
                사용자의 현재 관심사를 가장 잘 대표하는 핵심 키워드 3-5개를 쉼표로 구분하여 나열하세요.
                - 구체적이고 검색 의도가 명확한 키워드만 선택
                - BM25 검색에 사용되므로 검색어로 자주 쓰일 만한 용어 선택
                - 예: Kubernetes, React hooks, 분산 트랜잭션, 성능 최적화, MSA
                - 영문과 한글 혼용 가능

                데이터가 부족한 경우 관심 기술 스택을 기반으로 일반적인 프로필을 생성해주세요.
                """,
                formatList(data.interests()),
                formatPostDataList(data.readPostData()),
                formatPostDataList(data.bookmarkedPostData()),
                formatList(data.searchQueries())
        );
    }

    private String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- (데이터 없음)";
        }
        return items.stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
    }

    private String formatPostDataList(List<PostActivityData> postDataList) {
        if (postDataList == null || postDataList.isEmpty()) {
            return "- (데이터 없음)";
        }
        return postDataList.stream()
                .map(postData -> {
                    String keywordsStr = postData.keywords().isEmpty()
                            ? ""
                            : " [키워드: " + String.join(", ", postData.keywords()) + "]";
                    String engagementStr = postData.readingEngagement() != null
                            ? " (" + postData.readingEngagement() + ")"
                            : "";
                    return "- " + postData.title() + keywordsStr + engagementStr;
                })
                .collect(Collectors.joining("\n"));
    }

    private PersonalizationProfileAnalysis parseProfileAndKeywords(String llmResponse) {
        String profileText = "";
        List<String> keyKeywords = List.of();

        try {
            // PROFILE 섹션 추출
            int profileStart = llmResponse.indexOf("### PROFILE");
            int keywordsStart = llmResponse.indexOf("### KEYWORDS");

            if (profileStart != -1 && keywordsStart != -1) {
                profileText = llmResponse.substring(profileStart + "### PROFILE".length(), keywordsStart)
                        .trim();

                String keywordsSection = llmResponse.substring(keywordsStart + "### KEYWORDS".length())
                        .trim();

                // 쉼표로 구분된 키워드 파싱
                keyKeywords = Arrays.stream(keywordsSection.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(5)  // 최대 5개
                        .toList();
            } else {
                // 파싱 실패 시 전체 텍스트를 프로필로 사용
                log.warn("Failed to parse LLM response sections, using full text as profile");
                profileText = llmResponse;
            }
        } catch (Exception e) {
            log.error("Error parsing LLM response", e);
            profileText = llmResponse;
        }

        return new PersonalizationProfileAnalysis(profileText, keyKeywords);
    }
}
