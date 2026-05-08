CREATE TABLE IF NOT EXISTS first_read_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_read_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_first_read_posts_user_post (user_id, post_id),
    CONSTRAINT fk_first_read_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_first_read_posts_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO first_read_posts (first_read_at, user_id, post_id)
SELECT source.first_read_at, source.user_id, source.post_id
FROM (
    SELECT MIN(rp.read_at) AS first_read_at, rp.user_id, rp.post_id
    FROM read_posts rp
    GROUP BY rp.user_id, rp.post_id
) source
LEFT JOIN first_read_posts target
    ON target.user_id = source.user_id
   AND target.post_id = source.post_id
WHERE target.id IS NULL;
