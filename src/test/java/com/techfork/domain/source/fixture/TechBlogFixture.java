package com.techfork.domain.source.fixture;

import com.techfork.domain.source.entity.TechBlog;
import org.springframework.test.util.ReflectionTestUtils;

public final class TechBlogFixture {

    private TechBlogFixture() {
    }

    public static TechBlog createTechBlog(Long id, String company) {
        TechBlog techBlog = createTechBlog(
                company,
                "https://%s.example.com".formatted(company.toLowerCase()),
                "https://%s.example.com/rss".formatted(company.toLowerCase()),
                "https://cdn.example.com/%s.png".formatted(company.toLowerCase())
        );
        ReflectionTestUtils.setField(techBlog, "id", id);
        return techBlog;
    }

    public static TechBlog createTechBlog(String companyName, String blogUrl) {
        return createTechBlog(
                companyName,
                blogUrl,
                blogUrl + "/rss",
                blogUrl + "/logo.png"
        );
    }

    public static TechBlog createTechBlog(String companyName, String blogUrl, String rssUrl, String logoUrl) {
        return TechBlog.create(companyName, blogUrl, rssUrl, logoUrl);
    }
}
