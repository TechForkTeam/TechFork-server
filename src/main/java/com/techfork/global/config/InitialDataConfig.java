package com.techfork.global.config;

import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialDataConfig {

    private final TechBlogRepository techBlogRepository;

    @Bean
    public CommandLineRunner initTechBlogs(){
        return args -> {
            if (techBlogRepository.count() > 0) {
                log.info("TechBlog 데이터가 이미 존재합니다. ({}개) 스킵합니다.", techBlogRepository.count());
                return;
            }

            log.info("TechBlog 초기 데이터 삽입 시작...");

            List<TechBlog> techBlogs = List.of(
                    TechBlog.create("무신사", "https://medium.com/musinsa-tech", "https://medium.com/feed/musinsa-tech", "https://miro.medium.com/v2/resize:fill:160:160/1*Qs-0adxK8doDYyzZXMXkmg.png"),
                    TechBlog.create("HYPERCONNECT", "https://hyperconnect.github.io", "https://hyperconnect.github.io/feed.xml", null),
                    TechBlog.create("여기어때", "https://techblog.gccompany.co.kr", "https://techblog.gccompany.co.kr/rss", "https://miro.medium.com/v2/resize:fill:128:128/1*rGdUGkMoxT5SfrVKVAqbEw.png"),
                    TechBlog.create("인프랩", "https://tech.inflab.com", "https://tech.inflab.com/rss.xml", null),

                    TechBlog.create("네이버D2", "https://d2.naver.com/home", "https://d2.naver.com/d2.atom", " "),
                    TechBlog.create("요기요", "https://techblog.yogiyo.co.kr", "https://techblog.yogiyo.co.kr/feed", "https://miro.medium.com/v2/resize:fill:128:128/1*yMAs19hb_LGVSSVcalnkHw.jpeg"),
                    TechBlog.create("올리브영", "https://oliveyoung.tech", "https://oliveyoung.tech/rss.xml", null),
                    TechBlog.create("쏘카", "https://tech.socarcorp.kr/", "https://tech.socarcorp.kr/feed", null),

                    TechBlog.create("당근마켓", "https://medium.com/daangn", "https://medium.com/feed/daangn", "https://miro.medium.com/v2/resize:fill:128:128/1*Bm8_nGjfNiKV0PASwiPELg.png"),
                    TechBlog.create("29CM", "https://medium.com/29cm", "https://medium.com/feed/29cm", "https://miro.medium.com/v2/resize:fill:128:128/1*TP1aY6ZJaPSPs3fKA6sYKA.png"),
                    TechBlog.create("우아한형제들", "https://techblog.woowahan.com", "https://techblog.woowahan.com/feed/", null),
                    TechBlog.create("토스", "https://toss.tech", "https://toss.tech/rss.xml", null),
                    TechBlog.create("리디", "https://www.ridicorp.com/story-category/tech-blog/", "https://www.ridicorp.com/story-category/tech-blog/feed/", null),

                    TechBlog.create("SK C&C", "https://engineering-skcc.github.io", "https://engineering-skcc.github.io/feed.xml", " "),
                    TechBlog.create("카카오엔터프라이즈", "https://tech.kakaoenterprise.com", "https://tech.kakaoenterprise.com/feed", null),
                    TechBlog.create("NHN", "https://meetup.nhncloud.com", "https://meetup.nhncloud.com/rss", null),
                    TechBlog.create("원티드", "https://medium.com/wantedjobs", "https://medium.com/feed/wantedjobs", "https://miro.medium.com/v2/resize:fill:128:128/1*FJkqW-xbLo_-zwoSzR1C6Q.jpeg"),
                    TechBlog.create("SSG", "https://medium.com/ssgtech", "https://medium.com/feed/ssgtech", "https://miro.medium.com/v2/resize:fill:128:128/1*IFPbUqRn__nXbkPrxhi38A.png"),

                    TechBlog.create("왓챠", "https://medium.com/watcha", "https://medium.com/feed/watcha", "https://miro.medium.com/v2/resize:fill:128:128/1*3YwZ4pRivcBi8nWl7u98gA.png"),
                    TechBlog.create("롯데ON", "https://techblog.lotteon.com", "https://techblog.lotteon.com/feed", "https://miro.medium.com/v2/resize:fill:128:128/1*_xLrBUD-sQT0-URzwwZcAA.png"),
                    TechBlog.create("네이버 플레이스", "https://medium.com/naver-place-dev", "https://medium.com/feed/naver-place-dev", "https://miro.medium.com/v2/resize:fill:128:128/1*eQjirn85ojpPPzCAw9fU1g.png"),
                    TechBlog.create("데브시스터즈", "https://tech.devsisters.com", "https://tech.devsisters.com/rss.xml", null),

                    TechBlog.create("지마켓", "https://dev.gmarket.com", "https://dev.gmarket.com/rss", null),
                    TechBlog.create("번개장터", "https://medium.com/bunjang-tech-blog", "https://medium.com/feed/bunjang-tech-blog", "https://miro.medium.com/v2/resize:fill:128:128/1*XQ-7fuly8bBP4EVwj3ESbA.jpeg"),
                    TechBlog.create("스포카", "https://spoqa.github.io", "https://spoqa.github.io/rss", null),
                    TechBlog.create("줌인터넷", "https://zuminternet.github.io", "https://zuminternet.github.io/feed.xml", null),
                    TechBlog.create("AWS 한국", "https://aws.amazon.com/ko/blogs/tech/", "https://aws.amazon.com/ko/blogs/tech/feed", null)
            );

            techBlogRepository.saveAll(techBlogs);

            log.info("✅ TechBlog 초기 데이터 삽입 완료: 총 {}개", techBlogs.size());

            techBlogs.forEach(blog ->
                    log.debug("  - {}: {}", blog.getCompanyName(), blog.getRssUrl())
            );
        };
    }
}
