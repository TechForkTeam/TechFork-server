package com.techfork.global.util;

import com.techfork.global.config.CloudflareThirdPartyThumbnailOptimizationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudflareThirdPartyThumbnailOptimizerTest {

    @Test
    @DisplayName("활성화되면 외부 썸네일 URL을 Cloudflare 이미지 변환 URL로 바꾼다")
    void optimize_ReturnsCloudflareImageUrl_ForAbsoluteHttpUrl() {
        CloudflareThirdPartyThumbnailOptimizationProperties properties = new CloudflareThirdPartyThumbnailOptimizationProperties(
                true,
                "https://api.techfork.com",
                360,
                70,
                "scale-down",
                "auto"
        );

        CloudflareThirdPartyThumbnailOptimizer optimizer = new CloudflareThirdPartyThumbnailOptimizer(properties);

        String optimizedUrl = optimizer.optimize("https://images.example.com/thumb.jpg");

        assertThat(optimizedUrl)
                .isEqualTo("https://api.techfork.com/cdn-cgi/image/fit=scale-down,width=360,quality=70,format=auto/https://images.example.com/thumb.jpg");
    }

    @Test
    @DisplayName("비활성화되면 기존 URL을 그대로 유지한다")
    void optimize_ReturnsOriginalUrl_WhenDisabled() {
        CloudflareThirdPartyThumbnailOptimizationProperties properties = new CloudflareThirdPartyThumbnailOptimizationProperties();
        CloudflareThirdPartyThumbnailOptimizer optimizer = new CloudflareThirdPartyThumbnailOptimizer(properties);

        String originalUrl = "https://images.example.com/thumb.jpg";

        assertThat(optimizer.optimize(originalUrl)).isEqualTo(originalUrl);
    }

    @Test
    @DisplayName("이미 Cloudflare 변환 경로면 다시 감싸지 않는다")
    void optimize_ReturnsOriginalUrl_WhenAlreadyOptimized() {
        CloudflareThirdPartyThumbnailOptimizationProperties properties = new CloudflareThirdPartyThumbnailOptimizationProperties();
        properties.setEnabled(true);

        CloudflareThirdPartyThumbnailOptimizer optimizer = new CloudflareThirdPartyThumbnailOptimizer(properties);

        String originalUrl = "https://api.techfork.com/cdn-cgi/image/width=480,format=auto/https://images.example.com/thumb.jpg";

        assertThat(optimizer.optimize(originalUrl)).isEqualTo(originalUrl);
    }

    @Test
    @DisplayName("상대 경로나 비정상 URL은 건드리지 않는다")
    void optimize_ReturnsOriginalUrl_ForNonAbsoluteUrl() {
        CloudflareThirdPartyThumbnailOptimizationProperties properties = new CloudflareThirdPartyThumbnailOptimizationProperties();
        properties.setEnabled(true);

        CloudflareThirdPartyThumbnailOptimizer optimizer = new CloudflareThirdPartyThumbnailOptimizer(properties);

        assertThat(optimizer.optimize("/images/thumb.jpg")).isEqualTo("/images/thumb.jpg");
        assertThat(optimizer.optimize("thumb.jpg")).isEqualTo("thumb.jpg");
    }
}
