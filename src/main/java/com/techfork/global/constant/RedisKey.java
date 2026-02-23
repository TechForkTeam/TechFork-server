package com.techfork.global.constant;

public final class RedisKey {
    private RedisKey() {}

    public static final String REFRESH_TOKEN_PREFIX = "refreshToken:";
    public static final String USER_AUTH_PREFIX = "user:auth:";
    public static final String CRAWLING_LOCK_KEY = "rss-crawling";
}
