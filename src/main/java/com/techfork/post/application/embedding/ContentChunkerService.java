package com.techfork.post.application.embedding;

import com.techfork.global.util.ContentCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 및 Markdown 기반 fullContent를 의미 있는 청크로 분할하는 서비스
 * - RSS 피드는 대부분 HTML 형식
 * - <h1>~<h6> 태그로 섹션 분할
 * - <p> 태그로 세부 분할
 */
@Slf4j
@Service
public class ContentChunkerService {

    private static final int MAX_CHUNK_SIZE = 4000;
    private static final int MIN_CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 400;


    // HTML 헤더 패턴 (h1~h6 태그로 섹션 시작 위치 찾기)
    private static final Pattern HTML_HEADER = Pattern.compile("<h[1-6][^>]*>", Pattern.CASE_INSENSITIVE);

    // 마크다운 헤더 패턴 (#, ## 등)
    // (m) 플래그: ^가 각 줄의 시작과 일치하도록 함
    private static final Pattern MARKDOWN_HEADER = Pattern.compile("(?m)^\\s*#{1,6}\\s+");

    /**
     * HTML 컨텐츠를 청크로 분할
     */
    public List<String> chunkContent(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> chunks = chunkByStructure(content);

        // 청크 크기 조정 및 오버랩 추가
        return adjustChunkSizes(chunks);
    }

    /**
     * HTML 구조 기반 청크 분할
     * 1. <h1>~<h6> 태그로 섹션 분할
     * 2. HTML/Markdown 태그 제거하여 순수 텍스트 추출
     */
    private List<String> chunkByStructure(String content) {
        List<String> chunks = new ArrayList<>();

        Set<Integer> startSet = new TreeSet<>();
        startSet.add(0);

        // 1. HTML 헤더 태그(<h1>~<h6>)로 시작 위치 찾기
        Matcher headerMatcher = HTML_HEADER.matcher(content);
        while (headerMatcher.find()) {
            startSet.add(headerMatcher.start());
        }

        // 2. Markdown 헤더 위치 찾기
        Matcher mdMatcher = MARKDOWN_HEADER.matcher(content);
        while (mdMatcher.find()) {
            startSet.add(mdMatcher.start());
        }

        startSet.add(content.length());

        List<Integer> sectionStarts = new ArrayList<>(startSet);

        // 각 섹션 추출
        for (int i = 0; i < sectionStarts.size() - 1; i++) {
            int start = sectionStarts.get(i);
            int end = sectionStarts.get(i + 1);
            String sectionMarkup = content.substring(start, end).trim();

            if (!sectionMarkup.isEmpty()) {
                // HTML 및 마크다운 태그 제거하여 순수 텍스트로 변환
                String cleanText = ContentCleaner.clean(sectionMarkup);
                if (!cleanText.isBlank()) {
                    chunks.add(cleanText);
                }
            }
        }

        // 헤더가 없는 경우 (헤더 없이 바로 본문 시작)
        if (chunks.isEmpty()) {
            String cleanContent = ContentCleaner.clean(content);
            if (!cleanContent.isBlank()) {
                chunks.add(cleanContent);
            }
        }

        return chunks;
    }

    /**
     * 청크 크기 조정 및 오버랩 추가
     */
    private List<String> adjustChunkSizes(List<String> chunks) {
        List<String> adjustedChunks = new ArrayList<>();

        for (String chunk : chunks) {
            // 청크가 최대 크기보다 크면 분할
            if (chunk.length() > MAX_CHUNK_SIZE) {
                adjustedChunks.addAll(splitLargeChunk(chunk));
            } else if (chunk.length() >= MIN_CHUNK_SIZE) {
                adjustedChunks.add(chunk);
            } else {
                // 너무 작은 청크는 이전 청크와 병합
                if (!adjustedChunks.isEmpty()) {
                    int lastIndex = adjustedChunks.size() - 1;
                    String merged = adjustedChunks.get(lastIndex) + "\n\n" + chunk;

                    // 병합된 쳥크가 최대 크기를 넘지 않도록 방어 로직 추가
                    if (merged.length() <= MAX_CHUNK_SIZE * 1.1) { // 약간의 여유 허용
                        adjustedChunks.set(lastIndex, merged);
                    }
                } else {
                    adjustedChunks.add(chunk);
                }
            }
        }

        return adjustedChunks;
    }

    /**
     * 큰 청크를 여러 개로 분할
     */
    private List<String> splitLargeChunk(String chunk) {
        List<String> splits = new ArrayList<>();
        String[] paragraphs = chunk.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > MAX_CHUNK_SIZE) {
                if (currentChunk.length() > 0) {
                    splits.add(currentChunk.toString().trim());
                    // 오버랩을 위해 마지막 부분 일부 유지
                    currentChunk = new StringBuilder(getOverlapText(currentChunk.toString()));
                    currentChunk.append("\n\n");
                }
            }

            // 만약 단락 자체가 최대 크기보다 크다면, 강제로 분할 (간단한 처리)
            if (paragraph.length() > MAX_CHUNK_SIZE) {
                // 현재 청크가 있다면 먼저 추가
                if (currentChunk.length() > 0) {
                    splits.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                // 큰 단락을 분할해서 추가
                splits.addAll(forceSplitText(paragraph, MAX_CHUNK_SIZE, OVERLAP_SIZE));
            } else {
                currentChunk.append(paragraph).append("\n\n");
            }
        }

        if (currentChunk.length() > 0) {
            splits.add(currentChunk.toString().trim());
        }

        return splits;
    }

    /**
     * 오버랩용 텍스트 추출 (마지막 N자)
     */
    private String getOverlapText(String text) {
        if (text.length() <= OVERLAP_SIZE) {
            return text;
        }
        return text.substring(text.length() - OVERLAP_SIZE);
    }

    private List<String> forceSplitText(String text, int size, int overlap) {
        List<String> parts = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + size, length);
            parts.add(text.substring(start, end));
            start += (size - overlap);
            if (start >= length) break;
            // 오버랩이 너무 커서 무한 루프 도는 것 방지
            if (start <= end - size + overlap) {
                start = end; // 오버랩 없이 다음으로 넘어감
            }
        }
        return parts;
    }
}
