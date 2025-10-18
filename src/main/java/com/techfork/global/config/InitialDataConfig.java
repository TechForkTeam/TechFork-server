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
                    TechBlog.create("무신사", "https://medium.com/musinsa-tech", "https://medium.com/feed/musinsa-tech"),
                    TechBlog.create("라인", "https://engineering.linecorp.com/ko/blog", "https://engineering.linecorp.com/ko/feed/"),
                    TechBlog.create("HYPERCONNECT", "https://hyperconnect.github.io", "https://hyperconnect.github.io/feed.xml"),
                    TechBlog.create("여기어때", "https://techblog.gccompany.co.kr", "https://techblog.gccompany.co.kr/rss"),
                    TechBlog.create("인프랩", "https://tech.inflab.com", "https://tech.inflab.com/rss.xml"),

                    TechBlog.create("네이버D2", "https://d2.naver.com/home", "https://d2.naver.com/d2.atom"),
                    TechBlog.create("쿠팡", "https://medium.com/coupang-engineering/kr", "https://medium.com/feed/coupang-engineering/kr"),
                    TechBlog.create("요기요", "https://techblog.yogiyo.co.kr", "https://techblog.yogiyo.co.kr/feed"),
                    TechBlog.create("넷마블", "https://netmarble.engineering", "https://netmarble.engineering/feed/"),
                    TechBlog.create("올리브영", "https://oliveyoung.tech", "https://oliveyoung.tech/rss.xml"),

                    TechBlog.create("마켓컬리", "https://helloworld.kurly.com", "https://helloworld.kurly.com/feed.xml"),
                    TechBlog.create("당근마켓", "https://medium.com/daangn", "https://medium.com/feed/daangn"),
                    TechBlog.create("쏘카", "https://tech.socarcorp.kr", "https://tech.socarcorp.kr/rss.xml"),
                    TechBlog.create("29CM", "https://medium.com/29cm", "https://medium.com/feed/29cm"),

                    TechBlog.create("우아한형제들", "https://techblog.woowahan.com", "https://techblog.woowahan.com/feed/"),
                    TechBlog.create("토스", "https://toss.tech", "https://toss.tech/rss.xml"),
                    TechBlog.create("리디", "https://www.ridicorp.com/story-category/tech-blog/", "https://www.ridicorp.com/story-category/tech-blog/feed/"),
                    TechBlog.create("SK C&C", "https://engineering-skcc.github.io", "https://engineering-skcc.github.io/feed.xml"),

                    TechBlog.create("카카오엔터프라이즈", "https://tech.kakaoenterprise.com", "https://tech.kakaoenterprise.com/feed"),
                    TechBlog.create("직방", "https://medium.com/zigbang", "https://medium.com/feed/zigbang"),
                    TechBlog.create("NHN", "https://meetup.nhncloud.com", "https://meetup.nhncloud.com/rss"),
                    TechBlog.create("원티드", "https://medium.com/wantedjobs", "https://medium.com/feed/wantedjobs"),

                    TechBlog.create("카카오", "https://tech.kakao.com/blog", "https://tech.kakao.com/feed/"),
                    TechBlog.create("왓챠", "https://medium.com/watcha", "https://medium.com/feed/watcha"),
                    TechBlog.create("롯데ON", "https://techblog.lotteon.com", "https://techblog.lotteon.com/feed"),
                    TechBlog.create("네이버 플레이스", "https://medium.com/naver-place-dev", "https://medium.com/feed/naver-place-dev"),

                    TechBlog.create("데브시스터즈", "https://tech.devsisters.com", "https://tech.devsisters.com/rss.xml"),
                    TechBlog.create("뱅크샐러드", "https://blog.banksalad.com/tech", "https://blog.banksalad.com/rss.xml"),
                    TechBlog.create("지마켓", "https://dev.gmarket.com", "https://dev.gmarket.com/rss"),
                    TechBlog.create("야놀자", "https://yanolja.github.io", "https://yanolja.github.io/feed.xml")
            );

            techBlogRepository.saveAll(techBlogs);

            log.info("✅ TechBlog 초기 데이터 삽입 완료: 총 {}개", techBlogs.size());

            techBlogs.forEach(blog ->
                    log.debug("  - {}: {}", blog.getCompanyName(), blog.getRssUrl())
            );
        };
    }
}
