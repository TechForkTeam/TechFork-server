package com.techfork.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * JDBC Batch 실행을 위한 유틸리티 클래스
 * INSERT, UPDATE, DELETE를 배치로 처리하여 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcBatchExecutor {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Batch 쿼리 실행 (INSERT, UPDATE, DELETE 모두 지원)
     *
     * @param sql SQL 쿼리 (PreparedStatement 형식)
     * @param items 처리할 데이터 리스트
     * @param setter 각 항목에 대한 PreparedStatement 설정 로직
     * @param <T> 데이터 타입
     * @return 실제로 처리된 행의 수
     */
    public <T> int batchExecute(String sql, List<T> items, BatchParameterSetter<T> setter) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int[] updateCounts = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setter.setValues(ps, items.get(i), i);
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        int totalInserted = 0;
        for (int count : updateCounts) {
            if (count > 0) {
                totalInserted += count;
            }
        }

        log.debug("Batch operation 완료: {}개 항목 중 {}개 처리됨", items.size(), totalInserted);
        return totalInserted;
    }

    /**
     * PreparedStatement에 파라미터를 설정하는 함수형 인터페이스
     *
     * @param <T> 데이터 타입
     */
    @FunctionalInterface
    public interface BatchParameterSetter<T> {
        /**
         * PreparedStatement에 파라미터 설정
         *
         * @param ps PreparedStatement
         * @param item 현재 항목
         * @param index 현재 인덱스
         * @throws SQLException SQL 예외
         */
        void setValues(PreparedStatement ps, T item, int index) throws SQLException;
    }

    /**
     * Timestamp 변환 헬퍼 메서드
     */
    public static Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime != null ? Timestamp.valueOf(dateTime) : null;
    }
}
