package com.techfork.domain.source.batch;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RssFeedReader implements ItemReader<RssFeedItem> {

    private final TechBlogRepository techBlogRepository;

    private Iterator<TechBlog> techBlogIterator;
    private Iterator<RssFeedItem> currentBlogItemIterator;

    @Override
    public RssFeedItem read() {
        // 현재 블로그의 아이템이 남아있으면 반환
        if (currentBlogItemIterator != null && currentBlogItemIterator.hasNext()) {
            return currentBlogItemIterator.next();
        }

        // 첫 실행이거나 다음 블로그로 넘어가야 하는 경우
        if (techBlogIterator == null) {
            initializeTechBlogIterator();
        }

        // 다음 블로그 처리
        while (techBlogIterator.hasNext()) {
            TechBlog techBlog = techBlogIterator.next();
            try {
                List<RssFeedItem> items = fetchRssFeed(techBlog);
                if (!items.isEmpty()) {
                    currentBlogItemIterator = items.iterator();
                    log.info("[{}] RSS 수집 성공: {}개 아이템", techBlog.getCompanyName(), items.size());
                    return currentBlogItemIterator.next();
                } else {
                    log.warn("[{}] RSS 피드에 아이템이 없습니다", techBlog.getCompanyName());
                }
            } catch (Exception e) {
                log.error("[{}] RSS 수집 실패: {}", techBlog.getCompanyName(), e.getMessage(), e);
                // 실패해도 다음 블로그 계속 처리
            }
        }

        // 모든 블로그 처리 완료
        log.info("모든 RSS 피드 수집 완료");
        return null;
    }

    private void initializeTechBlogIterator() {
        List<TechBlog> techBlogs = techBlogRepository.findAll();
        this.techBlogIterator = techBlogs.iterator();
        log.info("총 {}개 테크 블로그 RSS 수집 시작", techBlogs.size());
    }

    private List<RssFeedItem> fetchRssFeed(TechBlog techBlog) throws Exception {
        URL url = new URL(techBlog.getRssUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // User-Agent 설정
            connection.setRequestProperty("User-Agent", "TechFork-Bot/1.0");
            connection.setConnectTimeout(10000); // 10초
            connection.setReadTimeout(10000);    // 10초

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP Error: " + responseCode);
            }

            // RSS 파싱
            try (InputStream inputStream = connection.getInputStream();
                 XmlReader reader = new XmlReader(inputStream)) {

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(reader);

                return feed.getEntries().stream()
                        .map(entry -> convertToFeedItem(entry, techBlog))
                        .collect(Collectors.toList());
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * SyndEntry를 RssFeedItem으로 변환
     */
    private RssFeedItem convertToFeedItem(SyndEntry entry, TechBlog techBlog) {
        // 본문 추출 (description 또는 content 중 더 긴 것 사용)
        String content = extractContent(entry);

        // 발행일 변환
        LocalDateTime publishedAt = convertToLocalDateTime(entry.getPublishedDate());

        // 태그 추출
        String tags = extractTags(entry);

        return RssFeedItem.builder()
                .title(entry.getTitle())
                .url(entry.getLink())
                .content(content)
                .publishedAt(publishedAt)
                .company(techBlog.getCompanyName())
                .techBlogId(techBlog.getId())
                .tags(tags)
                .build();
    }

    /**
     * RSS entry에서 본문 추출
     * description과 content:encoded 중 더 긴 것을 선택
     */
    private String extractContent(SyndEntry entry) {
        String description = entry.getDescription() != null
                ? entry.getDescription().getValue()
                : "";

        String content = "";
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            content = entry.getContents().get(0).getValue();
        }

        // 더 긴 것을 선택 (보통 content:encoded가 전체 본문)
        return content.length() > description.length() ? content : description;
    }

    /**
     * Date를 LocalDateTime으로 변환
     * publishedDate가 없으면 crawledAt 시점을 사용
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            log.debug("발행일이 없는 RSS 아이템 발견, 현재 시각 사용");
            return LocalDateTime.now();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * RSS entry에서 태그 추출
     */
    private String extractTags(SyndEntry entry) {
        if (entry.getCategories() == null || entry.getCategories().isEmpty()) {
            return "";
        }

        return entry.getCategories().stream()
                .map(SyndCategory::getName)
                .collect(Collectors.joining(","));
    }
}
