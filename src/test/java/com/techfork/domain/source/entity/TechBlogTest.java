package com.techfork.domain.source.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechBlogTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("logoUrl이 주어지면 전달한 값을 그대로 사용한다")
        void usesProvidedLogoUrlWhenPresent() {
            String logoUrl = "https://cdn.example.com/logo.png";

            TechBlog techBlog = TechBlog.create(
                    "TechFork",
                    "https://techfork.example.com",
                    "https://techfork.example.com/rss",
                    logoUrl
            );

            assertThat(techBlog.getCompanyName()).isEqualTo("TechFork");
            assertThat(techBlog.getBlogUrl()).isEqualTo("https://techfork.example.com");
            assertThat(techBlog.getRssUrl()).isEqualTo("https://techfork.example.com/rss");
            assertThat(techBlog.getLogoUrl()).isEqualTo(logoUrl);
        }

        @Test
        @DisplayName("logoUrl이 없으면 blogUrl 기반 기본 favicon URL을 생성한다")
        void generatesFallbackLogoUrlWhenLogoUrlMissing() {
            String blogUrl = "https://techfork.example.com";

            TechBlog techBlog = TechBlog.create(
                    "TechFork",
                    blogUrl,
                    "https://techfork.example.com/rss",
                    null
            );

            assertThat(techBlog.getLogoUrl()).isEqualTo(
                    "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://techfork.example.com/&size=128"
            );
        }
    }
}
