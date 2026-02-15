-- V1__init_schema.sql
-- 초기 스키마 생성: 12개 테이블 (FK 의존성 순서대로)

-- 1. tech_blogs (부모 테이블 - Post에서 참조)
CREATE TABLE IF NOT EXISTS tech_blogs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_name VARCHAR(100) NOT NULL,
    blog_url VARCHAR(500) NOT NULL,
    rss_url VARCHAR(500) NOT NULL,
    logo_url VARCHAR(500),
    last_crawled_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tech_blogs_blog_url (blog_url),
    UNIQUE KEY uk_tech_blogs_rss_url (rss_url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. users (부모 테이블 - 여러 테이블에서 참조)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nick_name VARCHAR(255),
    email VARCHAR(255),
    profile_image VARCHAR(255),
    description VARCHAR(255),
    social_type VARCHAR(255) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_social (social_type, social_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. posts (tech_blogs 참조)
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    full_content MEDIUMTEXT,
    plain_content MEDIUMTEXT,
    summary TEXT,
    short_summary TEXT,
    company VARCHAR(255) NOT NULL,
    logo_url VARCHAR(500),
    thumbnail_url VARCHAR(1000),
    url VARCHAR(1000) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    crawled_at DATETIME(6) NOT NULL,
    embedded_at DATETIME(6),
    view_count BIGINT NOT NULL DEFAULT 0,
    tech_blog_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_posts_url (url(768)),
    INDEX idx_post_published_at_id (published_at, id),
    INDEX idx_post_view_count_id (view_count, id),
    INDEX idx_post_company_published_at_id (company, published_at, id),
    CONSTRAINT fk_posts_tech_blog FOREIGN KEY (tech_blog_id) REFERENCES tech_blogs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. post_keywords (posts 참조)
CREATE TABLE IF NOT EXISTS post_keywords (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(50) NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_keywords_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. user_interest_categories (users 참조)
CREATE TABLE IF NOT EXISTS user_interest_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_interest_categories_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. user_interest_keywords (user_interest_categories 참조)
CREATE TABLE IF NOT EXISTS user_interest_keywords (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_interest_category_id BIGINT NOT NULL,
    keyword VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_interest_keywords_category FOREIGN KEY (user_interest_category_id) REFERENCES user_interest_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. read_posts (users, posts 참조)
CREATE TABLE IF NOT EXISTS read_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    read_at DATETIME(6) NOT NULL,
    read_duration_seconds INTEGER,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_read_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_read_posts_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. scrap_posts (users, posts 참조)
CREATE TABLE IF NOT EXISTS scrap_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scrapped_at DATETIME(6),
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scrap_posts_user_post (user_id, post_id),
    CONSTRAINT fk_scrap_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_scrap_posts_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. search_histories (users 참조)
CREATE TABLE IF NOT EXISTS search_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    search_word VARCHAR(200) NOT NULL,
    searched_at DATETIME(6),
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_search_histories_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. recommended_posts (users, posts 참조)
CREATE TABLE IF NOT EXISTS recommended_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    similarity_score DOUBLE NOT NULL,
    mmr_score DOUBLE NOT NULL,
    rank_order INTEGER NOT NULL,
    recommended_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommended_posts_user_post (user_id, post_id),
    INDEX idx_user_recommended_at (user_id, recommended_at DESC),
    CONSTRAINT fk_recommended_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recommended_posts_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. recommendation_history (users, posts 참조)
CREATE TABLE IF NOT EXISTS recommendation_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    similarity_score DOUBLE NOT NULL,
    mmr_score DOUBLE NOT NULL,
    rank_order INTEGER NOT NULL,
    recommended_at DATETIME(6) NOT NULL,
    is_clicked BIT(1) NOT NULL DEFAULT 0,
    clicked_at DATETIME(6),
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_recommended_at_hist (user_id, recommended_at DESC),
    INDEX idx_recommended_at_hist (recommended_at DESC),
    CONSTRAINT fk_recommendation_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recommendation_history_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. notification_token (users 참조)
CREATE TABLE IF NOT EXISTS notification_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(500) NOT NULL,
    is_active BIT(1) NOT NULL DEFAULT 1,
    user_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
