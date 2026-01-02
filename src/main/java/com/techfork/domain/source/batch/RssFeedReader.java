package com.techfork.domain.source.batch;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.global.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RssFeedReader implements ItemReader<RssFeedItem> {

    private final TechBlogRepository techBlogRepository;
    private final WebClient webClient;

    private ConcurrentLinkedQueue<RssFeedItem> itemQueue;

    @Override
    public RssFeedItem read() {
        // 첫 실행 시 모든 RSS 아이템을 큐에 추가
        if (itemQueue == null) {
            initializeQueue();
        }

        // 큐에서 아이템 꺼내기 (Thread-Safe)
        RssFeedItem item = itemQueue.poll();

        if (item == null) {
            log.info("모든 RSS 피드 수집 완료");
        }

        return item;
    }

    /**
     * 모든 RSS 피드를 미리 수집하여 큐에 저장
     * 한 번만 실행되며, 여러 스레드가 큐에서 안전하게 아이템을 가져감
     */
    private synchronized void initializeQueue() {
        // Double-checked locking
        if (itemQueue != null) return;

        itemQueue = new ConcurrentLinkedQueue<>();
        List<TechBlog> techBlogs = techBlogRepository.findAll();
        log.info("총 {}개 테크 블로그 RSS 수집 시작", techBlogs.size());

        List<RssFeedItem> allItems = techBlogs.parallelStream()
                .flatMap(techBlog -> {
                    try {
                        List<RssFeedItem> items = fetchRssFeed(techBlog);
                        log.info("[{}] RSS 수집 성공: {}개", techBlog.getCompanyName(), items.size());
                        return items.stream();
                    } catch (Exception e) {
                        log.error("[{}] RSS 수집 실패: {}", techBlog.getCompanyName(), e.getMessage());
                        return Stream.empty();
                    }
                })
                .toList();

        itemQueue.addAll(allItems);
        log.info("RSS 수집 초기화 완료: 총 {}개 아이템을 큐에 추가", allItems.size());
    }

    private List<RssFeedItem> fetchRssFeed(TechBlog techBlog) throws Exception {
        // WebClient로 RSS 피드 다운로드
        byte[] responseBytes = webClient.get()
                .uri(techBlog.getRssUrl())
                .retrieve()
                .bodyToMono(byte[].class)
                .block(); // 동기 처리 (Spring Batch에서는 동기 필요)

        if (responseBytes == null || responseBytes.length == 0) {
            throw new Exception("Empty response from RSS feed");
        }

        // RSS 파싱
        try (InputStream inputStream = new ByteArrayInputStream(responseBytes);
             XmlReader reader = new XmlReader(inputStream)) {

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(reader);

            return feed.getEntries().stream()
                    .map(entry -> convertToFeedItem(entry, techBlog))
                    .toList();
        }
    }

    /**
     * SyndEntry를 RssFeedItem으로 변환
     */
    private RssFeedItem convertToFeedItem(SyndEntry entry, TechBlog techBlog) {
        // 본문 추출 (description 또는 content 중 더 긴 것 사용)
        String content = extractContent(entry);

        // HTML 태그 및 마크다운 제거한 plain text 생성
        String plainContent = ContentCleaner.clean(content);

        // 발행일 변환
        LocalDateTime publishedAt = convertToLocalDateTime(entry.getPublishedDate());

        return RssFeedItem.builder()
                .title(entry.getTitle())
                .url(entry.getLink())
                .content(content)
                .plainContent(plainContent)
                .publishedAt(publishedAt)
                .company(techBlog.getCompanyName())
                .logoUrl(techBlog.getLogoUrl())
                .techBlogId(techBlog.getId())
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
            return LocalDateTime.now();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
