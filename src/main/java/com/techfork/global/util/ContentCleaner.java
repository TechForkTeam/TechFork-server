package com.techfork.global.util;

import java.util.regex.Pattern;

/**
 * HTML 태그와 마크다운 문법을 제거하는 유틸리티
 * Claude API에 전달하기 전 순수 텍스트만 추출
 */
public class ContentCleaner {

    // HTML 태그 제거
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    // HTML 엔티티 변환
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z]+;|&#[0-9]+;");

    // 마크다운 이미지 제거: ![alt](url)
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\([^)]+\\)");

    // 마크다운 링크를 텍스트만 남김: [text](url) -> text
    private static final Pattern MD_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");

    // 마크다운 헤딩 제거: ### Header -> Header
    private static final Pattern MD_HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);

    // 마크다운 코드 블록의 마크다운 구문만 제거하고 내용은 유지: ```lang\ncode\n``` -> code
    private static final Pattern MD_CODE_BLOCK_PATTERN = Pattern.compile("```\\w*\\n?([^`]*)```", Pattern.DOTALL);

    // 마크다운 인라인 코드 제거: `code`
    private static final Pattern MD_INLINE_CODE_PATTERN = Pattern.compile("`[^`]+`");

    // 마크다운 볼드/이탤릭 제거: **text**, __text__, *text*, _text_
    private static final Pattern MD_BOLD_ITALIC_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*|__([^_]+)__|\\*([^*]+)\\*|_([^_]+)_");

    // 연속된 공백을 하나로
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");

    // 연속된 줄바꿈을 두 개로 (단락 구분 유지)
    private static final Pattern MULTIPLE_NEWLINES_PATTERN = Pattern.compile("\\n{3,}");

    /**
     * HTML과 마크다운 문법을 제거하고 순수 텍스트만 반환
     *
     * @param content 원본 콘텐츠
     * @return 정제된 텍스트
     */
    public static String clean(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String cleaned = content;

        // 1. HTML 태그 제거
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll(" ");

        // 2. HTML 엔티티 변환 (간단한 것들만)
        cleaned = cleaned.replace("&nbsp;", " ")
                         .replace("&lt;", "<")
                         .replace("&gt;", ">")
                         .replace("&amp;", "&")
                         .replace("&quot;", "\"")
                         .replace("&#39;", "'");
        cleaned = HTML_ENTITY_PATTERN.matcher(cleaned).replaceAll(" ");

        // 3. 마크다운 코드 블록의 구문만 제거하고 내용은 유지
        cleaned = MD_CODE_BLOCK_PATTERN.matcher(cleaned).replaceAll("$1");

        // 4. 마크다운 인라인 코드는 내용만 남김
        cleaned = MD_INLINE_CODE_PATTERN.matcher(cleaned).replaceAll("$1");

        // 5. 마크다운 이미지 제거
        cleaned = MD_IMAGE_PATTERN.matcher(cleaned).replaceAll(" ");

        // 6. 마크다운 링크를 텍스트만 남김
        cleaned = MD_LINK_PATTERN.matcher(cleaned).replaceAll("$1");

        // 7. 마크다운 헤딩 기호 제거
        cleaned = MD_HEADING_PATTERN.matcher(cleaned).replaceAll("");

        // 8. 마크다운 볼드/이탤릭은 텍스트만 남김
        cleaned = MD_BOLD_ITALIC_PATTERN.matcher(cleaned).replaceAll("$1$2$3$4");

        // 9. 공백 정리
        cleaned = MULTIPLE_SPACES_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MULTIPLE_NEWLINES_PATTERN.matcher(cleaned).replaceAll("\n\n");

        return cleaned.trim();
    }

    /**
     * 길이 제한과 함께 정제
     *
     * @param content 원본 콘텐츠
     * @param maxLength 최대 길이
     * @return 정제되고 길이가 제한된 텍스트
     */
    public static String cleanAndLimit(String content, int maxLength) {
        String cleaned = clean(content);

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        // 단어 중간에서 자르지 않도록 마지막 공백 위치에서 자름
        int cutPosition = cleaned.lastIndexOf(' ', maxLength);
        if (cutPosition > maxLength * 0.9) { // 너무 많이 잘리지 않도록
            return cleaned.substring(0, cutPosition) + "...";
        }

        return cleaned.substring(0, maxLength) + "...";
    }
}
