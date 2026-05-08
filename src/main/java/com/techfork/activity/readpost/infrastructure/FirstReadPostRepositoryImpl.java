package com.techfork.activity.readpost.infrastructure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public class FirstReadPostRepositoryImpl implements FirstReadPostRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public FirstReadPostRepositoryImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean markFirstRead(Long userId, Long postId, LocalDateTime firstReadAt) {
        try {
            int affectedRows = jdbcTemplate.update("""
                    INSERT INTO first_read_posts (first_read_at, user_id, post_id)
                    VALUES (?, ?, ?)
                    """, Timestamp.valueOf(firstReadAt), userId, postId);
            return affectedRows == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
