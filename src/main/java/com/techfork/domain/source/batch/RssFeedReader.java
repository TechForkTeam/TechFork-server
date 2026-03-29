package com.techfork.domain.source.batch;

import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.global.util.ContentCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@StepScope
@Slf4j
public class RssFeedReader implements ItemReader<RssFeedItem> {

    private static final int RSS_FETCH_TASK_TIMEOUT_SECONDS = 45;

    private final TechBlogRepository techBlogRepository;
    private final PostRepository postRepository;
    private final WebClient webClient;
    @Qualifier("rssFetchTaskExecutor")
    private final AsyncTaskExecutor rssFetchTaskExecutor;
    private final int rssFetchTaskTimeoutSeconds;

    private List<RssFeedItem> items;
    private int currentIndex = 0;

    @Autowired
    public RssFeedReader(
            TechBlogRepository techBlogRepository,
            PostRepository postRepository,
            WebClient webClient,
            @Qualifier("rssFetchTaskExecutor") AsyncTaskExecutor rssFetchTaskExecutor
    ) {
        this(
                techBlogRepository,
                postRepository,
                webClient,
                rssFetchTaskExecutor,
                RSS_FETCH_TASK_TIMEOUT_SECONDS
        );
    }

    RssFeedReader(
            TechBlogRepository techBlogRepository,
            PostRepository postRepository,
            WebClient webClient,
            @Qualifier("rssFetchTaskExecutor") AsyncTaskExecutor rssFetchTaskExecutor,
            int rssFetchTaskTimeoutSeconds
    ) {
        this.techBlogRepository = techBlogRepository;
        this.postRepository = postRepository;
        this.webClient = webClient;
        this.rssFetchTaskExecutor = rssFetchTaskExecutor;
        this.rssFetchTaskTimeoutSeconds = rssFetchTaskTimeoutSeconds;
    }

    @Override
    public RssFeedItem read() {
        if (items == null) {
            initializeItems();
        }

        if (currentIndex >= items.size()) {
            log.info("모든 RSS 피드 수집 완료: 총 {}개", items.size());
            return null;
        }

        return items.get(currentIndex++);
    }

    private void initializeItems() {
        List<TechBlog> techBlogs = techBlogRepository.findAll();
        log.info("총 {}개 테크 블로그 RSS 수집 시작", techBlogs.size());

        List<FeedFetchTask> fetchTasks = techBlogs.stream()
                .map(this::submitFetchTask)
                .toList();

        List<RssFeedItem> allItems = fetchTasks.stream()
                .flatMap(this::collectFeedItems)
                .toList();

        Set<String> existingUrls = postRepository.findExistingUrls(
                allItems.stream().map(RssFeedItem::url).toList()
        );

        Map<String, RssFeedItem> uniqueItemsByUrl = new LinkedHashMap<>();
        for (RssFeedItem item : allItems) {
            if (!existingUrls.contains(item.url())) {
                uniqueItemsByUrl.putIfAbsent(item.url(), item);
            }
        }

        items = List.copyOf(uniqueItemsByUrl.values());

        log.info("RSS 수집 초기화 완료: 총 {}개 아이템", items.size());
    }

    private FeedFetchTask submitFetchTask(TechBlog techBlog) {
        Future<List<RssFeedItem>> future = rssFetchTaskExecutor.submit(() -> fetchFeedSafely(techBlog));
        return new FeedFetchTask(techBlog, future);
    }

    private List<RssFeedItem> fetchFeedSafely(TechBlog techBlog) {
        try {
            List<RssFeedItem> feedItems = fetchRssFeed(techBlog);
            log.info("[{}] RSS 수집 성공: {}개", techBlog.getCompanyName(), feedItems.size());
            return feedItems;
        } catch (Exception e) {
            log.error("[{}] RSS 수집 실패: {}", techBlog.getCompanyName(), e.getMessage());
            return List.of();
        }
    }

    private Stream<RssFeedItem> collectFeedItems(FeedFetchTask fetchTask) {
        try {
            return fetchTask.future()
                    .get(rssFetchTaskTimeoutSeconds, TimeUnit.SECONDS)
                    .stream();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[{}] RSS 수집 대기 중 인터럽트 발생", fetchTask.techBlog().getCompanyName(), e);
            return Stream.empty();
        } catch (TimeoutException e) {
            boolean cancelled = fetchTask.future().cancel(true);
            log.error("[{}] RSS 수집 타임아웃: {}초 (cancelled={})",
                    fetchTask.techBlog().getCompanyName(),
                    rssFetchTaskTimeoutSeconds,
                    cancelled);
            return Stream.empty();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("[{}] RSS 수집 Future 처리 실패: {}", fetchTask.techBlog().getCompanyName(), cause.getMessage());
            return Stream.empty();
        }
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

        // 썸네일 이미지 추출
        String thumbnailUrl = extractThumbnailUrl(entry, content);

        return RssFeedItem.builder()
                .title(entry.getTitle())
                .url(entry.getLink())
                .content(content)
                .plainContent(plainContent)
                .publishedAt(publishedAt)
                .company(techBlog.getCompanyName())
                .logoUrl(techBlog.getLogoUrl())
                .thumbnailUrl(thumbnailUrl)
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

    /**
     * RSS 피드에서 썸네일 이미지 URL 추출
     * 1. Media RSS 모듈 (media:thumbnail, media:content)
     * 2. Enclosure (주로 이미지/오디오 첨부파일)
     * 3. 본문 HTML에서 첫 번째 img 태그 추출
     */
    private String extractThumbnailUrl(SyndEntry entry, String content) {
        // 1. Media RSS 모듈에서 추출 시도
        String mediaThumbnail = extractFromMediaModule(entry);
        if (mediaThumbnail != null) {
            return mediaThumbnail;
        }

        // 2. Enclosure에서 이미지 추출 시도
        String enclosureImage = extractFromEnclosure(entry);
        if (enclosureImage != null) {
            return enclosureImage;
        }

        // 3. 본문 HTML에서 첫 번째 이미지 추출
        return extractImageFromHtml(content);
    }

    /**
     * Media RSS 모듈에서 이미지 추출
     */
    private String extractFromMediaModule(SyndEntry entry) {
        try {
            Object mediaModule = entry.getModule("http://search.yahoo.com/mrss/");
            if (mediaModule != null) {
                // Rome Tools의 Media RSS 모듈 사용
                MediaEntryModule media =
                        (MediaEntryModule) mediaModule;

                if (media.getMediaContents() != null && media.getMediaContents().length > 0) {
                    MediaContent mediaContent = media.getMediaContents()[0];
                    if (mediaContent.getReference() != null) {
                        return mediaContent.getReference().toString();
                    }
                }

                if (media.getMetadata() != null && media.getMetadata().getThumbnail() != null
                        && media.getMetadata().getThumbnail().length > 0) {
                    return media.getMetadata().getThumbnail()[0].getUrl().toString();
                }
            }
        } catch (Exception e) {
            log.debug("Media RSS 모듈에서 이미지 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Enclosure에서 이미지 추출
     */
    private String extractFromEnclosure(SyndEntry entry) {
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            for (SyndEnclosure enclosure : entry.getEnclosures()) {
                String type = enclosure.getType();
                if (type != null && type.startsWith("image/")) {
                    return enclosure.getUrl();
                }
            }
        }
        return null;
    }

    /**
     * HTML 본문에서 첫 번째 img 태그의 src 추출
     */
    private String extractImageFromHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return null;
        }

        // img 태그의 src 속성을 추출하는 정규식
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlContent);

        if (matcher.find()) {
            String imageUrl = matcher.group(1);
            // 상대 URL이 아닌 절대 URL만 반환 (http:// 또는 https://로 시작)
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                return imageUrl;
            }
        }

        return null;
    }

    private record FeedFetchTask(TechBlog techBlog, Future<List<RssFeedItem>> future) {
    }
}