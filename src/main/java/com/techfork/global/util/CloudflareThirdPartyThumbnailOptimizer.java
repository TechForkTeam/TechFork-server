package com.techfork.global.util;

import com.techfork.global.config.CloudflareThirdPartyThumbnailOptimizationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class CloudflareThirdPartyThumbnailOptimizer {

    private static final String CLOUDFLARE_IMAGE_PATH = "/cdn-cgi/image/";

    private final CloudflareThirdPartyThumbnailOptimizationProperties properties;

    public String optimize(String thumbnailUrl) {
        if (!properties.isEnabled() || !StringUtils.hasText(thumbnailUrl) || isAlreadyOptimized(thumbnailUrl)) {
            return thumbnailUrl;
        }

        if (!isAbsoluteHttpUrl(thumbnailUrl)) {
            return thumbnailUrl;
        }

        return normalizeDeliveryBaseUrl(properties.getDeliveryBaseUrl())
                + CLOUDFLARE_IMAGE_PATH
                + buildOptions()
                + "/"
                + thumbnailUrl;
    }

    private boolean isAlreadyOptimized(String thumbnailUrl) {
        return thumbnailUrl.contains(CLOUDFLARE_IMAGE_PATH);
    }

    private boolean isAbsoluteHttpUrl(String thumbnailUrl) {
        try {
            URI uri = URI.create(thumbnailUrl);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StringUtils.hasText(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String normalizeDeliveryBaseUrl(String deliveryBaseUrl) {
        if (!StringUtils.hasText(deliveryBaseUrl)) {
            return "";
        }

        return deliveryBaseUrl.endsWith("/")
                ? deliveryBaseUrl.substring(0, deliveryBaseUrl.length() - 1)
                : deliveryBaseUrl;
    }

    private String buildOptions() {
        StringBuilder options = new StringBuilder();

        appendOption(options, "fit", properties.getFit());
        appendOption(options, "width", properties.getWidth());
        appendOption(options, "quality", properties.getQuality());
        appendOption(options, "format", properties.getFormat());

        return options.toString();
    }

    private void appendOption(StringBuilder options, String key, Object value) {
        if (value == null) {
            return;
        }

        String stringValue = value.toString();
        if (!StringUtils.hasText(stringValue)) {
            return;
        }

        if (!options.isEmpty()) {
            options.append(',');
        }

        options.append(key).append('=').append(stringValue);
    }
}
