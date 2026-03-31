package com.techfork.global.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cloudflare.third-party-thumbnail-optimization")
public class CloudflareThirdPartyThumbnailOptimizationProperties {

    private boolean enabled = false;

    private String deliveryBaseUrl = "";

    private Integer width = 480;

    private Integer quality = 75;

    private String fit = "scale-down";

    private String format = "auto";
}
