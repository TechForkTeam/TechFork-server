-- Align search history storage with the SearchQuery terminology.
-- This migration is idempotent so environments already adjusted manually
-- or restored from a newer snapshot do not fail.

SET @rename_search_query_column = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'search_histories'
          AND column_name = 'search_word'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'search_histories'
          AND column_name = 'query'
    ),
    'ALTER TABLE search_histories RENAME COLUMN search_word TO query',
    'SELECT 1'
);
PREPARE rename_search_query_column_stmt FROM @rename_search_query_column;
EXECUTE rename_search_query_column_stmt;
DEALLOCATE PREPARE rename_search_query_column_stmt;
