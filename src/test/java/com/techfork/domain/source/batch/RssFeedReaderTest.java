package com.techfork.domain.source.batch;

import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("기존 URL을 제외하고 동일 crawl 내 중복 URL은 한 번만 반환한다")
    void read_FiltersExistingUrlsAndDeduplicatesCollectedItems() {
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

        RssFeedReader reader = new RssFeedReader(
                techBlogRepository,
                postRepository,
                webClient,
                rssFetchTaskExecutor,
                1
        );

        List<RssFeedItem> items = readAll(reader);

        assertThat(items)
                .extracting(RssFeedItem::url)
                .containsExactly("https://posts.example.com/1", "https://posts.example.com/2");
    }

    @Test
    @DisplayName("일부 feed 실패가 있어도 성공한 feed 결과는 유지한다")
    void read_KeepsSuccessfulItemsWhenOneFeedFails() {
        TechBlog kakao = TechBlog.create("카카오", "https://kakao.example.com", "https://kakao.example.com/rss", null);
        TechBlog naver = TechBlog.create("네이버", "https://naver.example.com", "https://naver.example.com/rss", null);

        given(techBlogRepository.findAll()).willReturn(List.of(kakao, naver));
        given(postRepository.findExistingUrls(anyList())).willReturn(Set.of());

        WebClient webClient = createWebClient(Map.of(
                kakao.getRssUrl(), successResponse(rssXml(rssItem("카카오 1", "https://posts.example.com/1", "본문1"))),
                naver.getRssUrl(), Mono.error(new RuntimeException("rss fetch failed"))
        ));

        RssFeedReader reader = new RssFeedReader(
                techBlogRepository,
                postRepository,
                webClient,
                rssFetchTaskExecutor,
                1
        );

        List<RssFeedItem> items = readAll(reader);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url()).isEqualTo("https://posts.example.com/1");
    }

    @Test
    @DisplayName("느린 feed는 timeout 처리되고 다른 feed 결과는 계속 반환한다")
    void read_TimesOutSlowFeedAndKeepsOtherResults() {
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

        RssFeedReader reader = new RssFeedReader(
                techBlogRepository,
                postRepository,
                webClient,
                rssFetchTaskExecutor,
                1
        );

        List<RssFeedItem> items = readAll(reader);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url()).isEqualTo("https://posts.example.com/1");
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

    private String rssXml(String... items) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <rss version=\"2.0\">
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
}
