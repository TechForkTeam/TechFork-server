package com.techfork.global.constant;

public final class Constants {
    private Constants() {}

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String JWT_EXCEPTION_ATTRIBUTE = "jwtException";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/login/**",
            "/oauth2/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error",
            "/api/v2/posts/**",
            "/api/v1/posts/**",
            "/api/v1/search/**",
            "/api/v1/onboarding/interests"
    };

    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**"
    };

}
