package com.techfork.domain.source.batch;

import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RssFeedReaderTest {

    @Mock
    private TechBlogRepository techBlogRepository;

    @Mock
    private PostRepository postRepository;

    private ThreadPoolTaskExecutor rssFetchTaskExecutor;

    @BeforeEach
    void setUp() {
        rssFetchTaskExecutor = new ThreadPoolTaskExecutor();
        rssFetchTaskExecutor.setCorePoolSize(2);
        rssFetchTaskExecutor.setMaxPoolSize(2);
        rssFetchTaskExecutor.setQueueCapacity(10);
        rssFetchTaskExecutor.setThreadNamePrefix("rss-fetch-test-");
        rssFetchTaskExecutor.initialize();
    }

    @AfterEach
    void tearDown() {
        rssFetchTaskExecutor.shutdown();
    }

    @Nested
    @DisplayName("초기 수집")
    class CollectionFlow {

        @Test
        @DisplayName("기존 URL을 제외하고 동일 crawl 내 중복 URL은 한 번만 반환한다")
        void filtersExistingUrlsAndDeduplicatesCollectedItems() {
            TechBlog kakao = TechBlog.create("카카오", "https://kakao.example.com", "https://kakao.example.com/rss", null);
            TechBlog naver = TechBlog.create("네이버", "https://naver.example.com", "https://naver.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(kakao, naver));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of("https://posts.example.com/existing"));

            WebClient webClient = createWebClient(Map.of(
                    kakao.getRssUrl(), successResponse(rssXml(
                            rssItem("카카오 1", "https://posts.example.com/1", "본문1"),
                            rssItem("카카오 existing", "https://posts.example.com/existing", "본문2")
                    )),
                    naver.getRssUrl(), successResponse(rssXml(
                            rssItem("네이버 duplicate", "https://posts.example.com/1", "본문3"),
                            rssItem("네이버 2", "https://posts.example.com/2", "본문4")
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items)
                    .extracting(RssFeedItem::url)
                    .containsExactly("https://posts.example.com/1", "https://posts.example.com/2");
        }
    }

    @Nested
    @DisplayName("장애 허용")
    class FailureTolerance {

        @Test
        @DisplayName("일부 feed 실패가 있어도 성공한 feed 결과는 유지한다")
        void keepsSuccessfulItemsWhenOneFeedFails() {
            TechBlog kakao = TechBlog.create("카카오", "https://kakao.example.com", "https://kakao.example.com/rss", null);
            TechBlog naver = TechBlog.create("네이버", "https://naver.example.com", "https://naver.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(kakao, naver));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    kakao.getRssUrl(), successResponse(rssXml(rssItem("카카오 1", "https://posts.example.com/1", "본문1"))),
                    naver.getRssUrl(), Mono.error(new RuntimeException("rss fetch failed"))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).url()).isEqualTo("https://posts.example.com/1");
        }

        @Test
        @DisplayName("느린 feed는 timeout 처리되고 다른 feed 결과는 계속 반환한다")
        void timesOutSlowFeedAndKeepsOtherResults() {
            TechBlog kakao = TechBlog.create("카카오", "https://kakao.example.com", "https://kakao.example.com/rss", null);
            TechBlog naver = TechBlog.create("네이버", "https://naver.example.com", "https://naver.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(kakao, naver));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    kakao.getRssUrl(), successResponse(rssXml(rssItem("카카오 1", "https://posts.example.com/1", "본문1"))),
                    naver.getRssUrl(), Mono.delay(java.time.Duration.ofMillis(1500))
                            .map(ignore -> ClientResponse.create(HttpStatus.OK)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                                    .body(rssXml(rssItem("네이버 1", "https://posts.example.com/2", "본문2")))
                                    .build())
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).url()).isEqualTo("https://posts.example.com/1");
        }

        @Test
        @DisplayName("빈 RSS 응답이면 결과 없이 종료한다")
        void returnsEmptyWhenFeedResponseIsEmpty() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), emptyResponse()
            ));

            RssFeedReader reader = newReader(webClient);

            assertThat(readAll(reader)).isEmpty();
        }

        @Test
        @DisplayName("피드 파싱에 실패해도 다른 피드 결과는 유지한다")
        void keepsOtherResultsWhenFeedParsingFails() {
            TechBlog kakao = TechBlog.create("카카오", "https://kakao.example.com", "https://kakao.example.com/rss", null);
            TechBlog naver = TechBlog.create("네이버", "https://naver.example.com", "https://naver.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(kakao, naver));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    kakao.getRssUrl(), successResponse(rssXml(rssItem("카카오 1", "https://posts.example.com/1", "본문1"))),
                    naver.getRssUrl(), successResponse("<rss><channel><item><title>broken")
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).url()).isEqualTo("https://posts.example.com/1");
        }
    }

    @Nested
    @DisplayName("본문/메타데이터 추출")
    class ContentAndMetadataExtraction {

        @Test
        @DisplayName("content:encoded가 description보다 길면 content를 본문으로 사용한다")
        void usesContentWhenItIsLongerThanDescription() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);
            String longContent = "<p>description 보다 훨씬 긴 content 본문입니다.</p>";

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItemWithContent("본문 선택", "https://posts.example.com/content", "짧은 설명", longContent)
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).content()).isEqualTo(longContent);
            assertThat(items.get(0).plainContent()).contains("content 본문");
        }

        @Test
        @DisplayName("content가 없거나 더 짧으면 description을 본문으로 사용한다")
        void usesDescriptionWhenContentIsMissingOrShorter() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);
            String description = "content 보다 더 긴 description 본문입니다.";

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItemWithContent("description 선택", "https://posts.example.com/description", description, "짧음")
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).content()).isEqualTo(description);
        }

        @Test
        @DisplayName("발행일이 없으면 현재 시각으로 fallback 한다")
        void fallsBackToNowWhenPublishedDateIsNull() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItemWithoutPublishedDate("발행일 없음", "https://posts.example.com/no-date", "본문")
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            LocalDateTime before = LocalDateTime.now();
            List<RssFeedItem> items = readAll(reader);
            LocalDateTime after = LocalDateTime.now();

            assertThat(items).hasSize(1);
            assertThat(items.get(0).publishedAt()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("썸네일 추출")
    class ThumbnailExtraction {

        @Test
        @DisplayName("media module 이미지가 있으면 thumbnail 우선순위 1번으로 사용한다")
        void extractsThumbnailFromMediaModuleFirst() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);
            String mediaImage = "https://cdn.example.com/media-thumbnail.jpg";

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItemWithMediaContent(
                                    "media 이미지",
                                    "https://posts.example.com/media",
                                    "<p><img src=\"https://cdn.example.com/html-thumbnail.jpg\" /></p>",
                                    mediaImage
                            )
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).thumbnailUrl()).isEqualTo(mediaImage);
        }

        @Test
        @DisplayName("media module이 없으면 enclosure 이미지를 thumbnail로 사용한다")
        void extractsThumbnailFromEnclosureWhenMediaMissing() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);
            String enclosureImage = "https://cdn.example.com/enclosure-thumbnail.jpg";

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItemWithEnclosure(
                                    "enclosure 이미지",
                                    "https://posts.example.com/enclosure",
                                    "<p>본문</p>",
                                    enclosureImage
                            )
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).thumbnailUrl()).isEqualTo(enclosureImage);
        }

        @Test
        @DisplayName("media와 enclosure가 모두 없으면 HTML의 첫 이미지로 thumbnail을 채운다")
        void extractsThumbnailFromHtmlWhenNoMediaOrEnclosureExists() {
            TechBlog techBlog = TechBlog.create("테크포크", "https://techfork.example.com", "https://techfork.example.com/rss", null);
            String htmlImage = "https://cdn.example.com/html-thumbnail.jpg";

            given(techBlogRepository.findAll()).willReturn(List.of(techBlog));
            given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

            WebClient webClient = createWebClient(Map.of(
                    techBlog.getRssUrl(), successResponse(rssXml(
                            rssItem("html 이미지", "https://posts.example.com/html", "<p><img src=\"" + htmlImage + "\" />본문</p>")
                    ))
            ));

            RssFeedReader reader = newReader(webClient);

            List<RssFeedItem> items = readAll(reader);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).thumbnailUrl()).isEqualTo(htmlImage);
        }
    }

    private RssFeedReader newReader(WebClient webClient) {
        return new RssFeedReader(
                techBlogRepository,
                postRepository,
                webClient,
                rssFetchTaskExecutor,
                1
        );
    }

    private List<RssFeedItem> readAll(RssFeedReader reader) {
        List<RssFeedItem> items = new ArrayList<>();
        RssFeedItem item;
        while ((item = reader.read()) != null) {
            items.add(item);
        }
        return items;
    }

    private WebClient createWebClient(Map<String, Mono<ClientResponse>> responses) {
        ExchangeFunction exchangeFunction = request -> {
            Mono<ClientResponse> response = responses.get(request.url().toString());
            if (response == null) {
                return Mono.error(new IllegalArgumentException("Unexpected request URL: " + request.url()));
            }
            return response;
        };

        return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
    }

    private Mono<ClientResponse> successResponse(String body) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .body(body)
                .build());
    }

    private Mono<ClientResponse> emptyResponse() {
        return Mono.just(ClientResponse.create(HttpStatus.OK).build());
    }

    private String rssXml(String... items) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <rss version=\"2.0\"
                     xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"
                     xmlns:media=\"http://search.yahoo.com/mrss/\">
                  <channel>
                    <title>Test Feed</title>
                    <link>https://feed.example.com</link>
                    <description>Test Feed</description>
                    %s
                  </channel>
                </rss>
                """.formatted(String.join(System.lineSeparator(), items));
    }

    private String rssItem(String title, String link, String description) {
        return """
                <item>
                  <title>%s</title>
                  <link>%s</link>
                  <description><![CDATA[%s]]></description>
                  <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
                </item>
                """.formatted(title, link, description);
    }

    private String rssItemWithContent(String title, String link, String description, String content) {
        return """
                <item>
                  <title>%s</title>
                  <link>%s</link>
                  <description><![CDATA[%s]]></description>
                  <content:encoded><![CDATA[%s]]></content:encoded>
                  <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
                </item>
                """.formatted(title, link, description, content);
    }

    private String rssItemWithoutPublishedDate(String title, String link, String description) {
        return """
                <item>
                  <title>%s</title>
                  <link>%s</link>
                  <description><![CDATA[%s]]></description>
                </item>
                """.formatted(title, link, description);
    }

    private String rssItemWithMediaContent(String title, String link, String description, String mediaUrl) {
        return """
                <item>
                  <title>%s</title>
                  <link>%s</link>
                  <description><![CDATA[%s]]></description>
                  <media:content url=\"%s\" medium=\"image\" />
                  <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
                </item>
                """.formatted(title, link, description, mediaUrl);
    }

    private String rssItemWithEnclosure(String title, String link, String description, String enclosureUrl) {
        return """
                <item>
                  <title>%s</title>
                  <link>%s</link>
                  <description><![CDATA[%s]]></description>
                  <enclosure url=\"%s\" type=\"image/jpeg\" />
                  <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
                </item>
                """.formatted(title, link, description, enclosureUrl);
    }
}
