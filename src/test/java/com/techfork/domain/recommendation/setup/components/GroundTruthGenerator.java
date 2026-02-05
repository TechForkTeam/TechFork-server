package com.techfork.domain.recommendation.setup.components;

import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.global.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ground Truth 계산 및 품질 검증
 * LLM-as-a-Judge 방식을 사용하여 정답 데이터를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroundTruthGenerator {

    private final PostDocumentRepository postDocumentRepository;
    private final LlmClient llmClient;

    /**
     * Ground Truth 관련도 점수 계산 (LLM 기반)
     *
     * @param posts       평가할 게시글 목록
     * @param userProfile 사용자 프로필 문서 (프로필 텍스트 포함)
     * @return 게시글 ID -> 관련도 점수 (1~5점)
     */
    public Map<Long, Integer> calculateGroundTruth(
            List<Post> posts,
            UserProfileDocument userProfile) {

        Map<Long, Integer> groundTruthScores = new HashMap<>();
        int count = 0;

        for (Post post : posts) {
            count++;
            log.info("Ground Truth 평가 중 ({}/{}): Post ID {}", count, posts.size(), post.getId());
            
            try {
                int score = calculateRelevanceScoreWithLLM(post, userProfile);
                groundTruthScores.put(post.getId(), score);
            } catch (Exception e) {
                log.error("LLM 평가 실패 (Post ID {}): {}", post.getId(), e.getMessage());
                // 실패 시 기본 점수 1점 부여 (안전장치)
                groundTruthScores.put(post.getId(), 1);
            }
        }

        return groundTruthScores;
    }

    /**
     * LLM을 사용하여 게시글의 관련도 점수 평가 (1~5점)
     */
    private int calculateRelevanceScoreWithLLM(Post post, UserProfileDocument userProfile) {
        // PostDocument에서 더 풍부한 정보 가져오기 (요약문, 본문 청크 등)
        Optional<PostDocument> postDocOpt = postDocumentRepository.findByPostId(post.getId());
        
        String postSummary = postDocOpt.map(PostDocument::getSummary).orElse(post.getSummary());
        
        // 본문 내용 일부 가져오기 (Content Chunks가 있다면 도입부와 결론부 위주로 추출)
        StringBuilder contentContext = new StringBuilder();
        if (postDocOpt.isPresent() && postDocOpt.get().getContentChunks() != null) {
            var chunks = postDocOpt.get().getContentChunks();
            if (!chunks.isEmpty()) {
                // 1. 도입부 (첫 번째 청크)
                String intro = chunks.get(0).getChunkText();
                contentContext.append("[도입부]\n")
                        .append(intro.substring(0, Math.min(intro.length(), 1500)))
                        .append("\n\n");

                // 2. 결론부 (마지막 청크 - 도입부와 다른 경우에만)
                if (chunks.size() > 1) {
                    String conclusion = chunks.get(chunks.size() - 1).getChunkText();
                    contentContext.append("[결론 및 요약]\n")
                            .append(conclusion.substring(0, Math.min(conclusion.length(), 1500)));
                }
            }
        }

        String systemPrompt = "당신은 기술 블로그 추천 시스템의 품질 평가자(Judge)입니다. 사용자 프로필과 게시글 내용을 바탕으로 적합성을 1~5점 척도로 평가하세요.";
        
        String userPrompt = String.format("""
                다음 사용자가 해당 게시글을 추천받았을 때 얼마나 만족할지 평가해주세요.
                
                ## 사용자 프로필
                %s
                
                ## 게시글 정보
                - 제목: %s
                - 회사/블로그: %s
                - 요약: %s
                - 본문 내용(일부):
                %s
                
                ## 평가 기준
                5점 (매우 강한 추천): 사용자의 핵심 관심사(주력 기술, 해결하려는 문제)와 정확히 일치하며, 반드시 읽어야 할 글.
                4점 (추천): 사용자의 관심사와 밀접하게 관련되어 있으며, 흥미를 느낄 만한 글.
                3점 (보통): 사용자의 관심사와 관련은 있으나, 핵심 분야가 아니거나 너무 일반적인 내용.
                2점 (약간 관련): 키워드는 일부 겹치지만, 사용자의 주된 관심사와 거리가 먼 글.
                1점 (관련 없음): 사용자의 관심사와 전혀 무관한 글.
                
                ## 응답 형식
                반드시 점수(숫자 1~5)만 출력하세요. 설명은 필요 없습니다.
                """,
                userProfile.getProfileText(),
                post.getTitle(),
                post.getCompany(),
                postSummary,
                contentContext.length() > 0 ? contentContext.toString() : "(본문 데이터 없음)"
        );

        String response = llmClient.call(systemPrompt, userPrompt);
        return parseScore(response);
    }

    private int parseScore(String response) {
        // 응답에서 숫자만 추출
        try {
            // "점수: 5" 같은 형식에 대비
            Matcher matcher = Pattern.compile("(\\d)").matcher(response);
            if (matcher.find()) {
                int score = Integer.parseInt(matcher.group(1));
                return Math.max(1, Math.min(5, score)); // 1~5 범위 제한
            }
            
            // 숫자를 못 찾은 경우
            log.warn("LLM 응답에서 점수를 파싱할 수 없음: {}", response);
            return 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
