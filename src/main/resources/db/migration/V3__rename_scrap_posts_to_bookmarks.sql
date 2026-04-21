-- Rename bookmark storage to the ubiquitous language.
-- Constraint/index symbols are intentionally left untouched because existing
-- environments can have different physical FK names. JPA validates the table
-- and column names, not FK/index symbol names.

SET @rename_bookmark_table = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'scrap_posts'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'bookmarks'
    ),
    'RENAME TABLE scrap_posts TO bookmarks',
    'SELECT 1'
);
PREPARE rename_bookmark_table_stmt FROM @rename_bookmark_table;
EXECUTE rename_bookmark_table_stmt;
DEALLOCATE PREPARE rename_bookmark_table_stmt;

SET @rename_bookmarked_at_column = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'bookmarks'
          AND column_name = 'scrapped_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'bookmarks'
          AND column_name = 'bookmarked_at'
    ),
    'ALTER TABLE bookmarks RENAME COLUMN scrapped_at TO bookmarked_at',
    'SELECT 1'
);
PREPARE rename_bookmarked_at_column_stmt FROM @rename_bookmarked_at_column;
EXECUTE rename_bookmarked_at_column_stmt;
DEALLOCATE PREPARE rename_bookmarked_at_column_stmt;
