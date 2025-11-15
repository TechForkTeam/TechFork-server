package com.techfork.domain.post.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 기반 fullContent를 의미 있는 청크로 분할하는 서비스
 * - RSS 피드는 대부분 HTML 형식
 * - <h1>~<h6> 태그로 섹션 분할
 * - <p> 태그로 세부 분할
 */
@Slf4j
@Service
public class ContentChunkerService {

    // 최대 청크 크기 (약 500 토큰)
    private static final int MAX_CHUNK_SIZE = 2000;

    // 최소 청크 크기 (너무 작은 청크 방지)
    private static final int MIN_CHUNK_SIZE = 100;

    // 청크 간 오버랩 크기
    private static final int OVERLAP_SIZE = 200;

    // HTML 헤더 패턴 (h1~h6 태그로 섹션 시작 위치 찾기)
    private static final Pattern HTML_HEADER = Pattern.compile("<h[1-6][^>]*>", Pattern.CASE_INSENSITIVE);

    // HTML 단락 패턴 (p 태그)
    private static final Pattern HTML_PARAGRAPH = Pattern.compile("<p[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * HTML 컨텐츠를 청크로 분할
     */
    public List<String> chunkContent(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> chunks = chunkByHtmlStructure(content);

        // 청크 크기 조정 및 오버랩 추가
        return adjustChunkSizes(chunks);
    }

    /**
     * HTML 구조 기반 청크 분할
     * 1. <h1>~<h6> 태그로 섹션 분할
     * 2. HTML 태그 제거하여 순수 텍스트 추출
     */
    private List<String> chunkByHtmlStructure(String content) {
        List<String> chunks = new ArrayList<>();

        // HTML 헤더 태그(<h1>~<h6>)로 섹션 시작 위치 찾기
        Matcher headerMatcher = HTML_HEADER.matcher(content);
        List<Integer> sectionStarts = new ArrayList<>();
        sectionStarts.add(0);

        while (headerMatcher.find()) {
            sectionStarts.add(headerMatcher.start());
        }
        sectionStarts.add(content.length());

        // 각 섹션 추출
        for (int i = 0; i < sectionStarts.size() - 1; i++) {
            int start = sectionStarts.get(i);
            int end = sectionStarts.get(i + 1);
            String sectionHtml = content.substring(start, end).trim();

            if (!sectionHtml.isEmpty()) {
                // HTML 태그 제거하여 순수 텍스트로 변환
                String cleanText = removeHtmlTags(sectionHtml);
                if (!cleanText.isBlank()) {
                    chunks.add(cleanText);
                }
            }
        }

        // 헤더가 없는 경우 (헤더 없이 바로 본문 시작)
        if (chunks.isEmpty()) {
            String cleanContent = removeHtmlTags(content);
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
                    adjustedChunks.set(lastIndex, merged);
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
            currentChunk.append(paragraph).append("\n\n");
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

    /**
     * HTML 태그 제거
     */
    private String removeHtmlTags(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
