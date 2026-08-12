package com.ghost616.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库 Schema 迁移基类，封装 ALTER TABLE 增量迁移与空值回填的公共逻辑。
 */
@Slf4j
public abstract class AbstractSchemaMigration {

    protected final JdbcTemplate jdbcTemplate;

    protected AbstractSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected record Migration(String tableName, String columnName, String columnType, String defaultValue) {
        String toAlterSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE \"").append(tableName).append("\" ADD COLUMN \"").append(columnName).append("\" ").append(columnType);
            if (defaultValue != null && !defaultValue.isEmpty()) {
                sql.append(" DEFAULT ").append(defaultValue);
            }
            return sql.toString();
        }
    }

    protected boolean isDuplicateColumn(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SQLException
                    && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("duplicate column name")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    protected void runAlterMigrations(List<Migration> migrations) {
        for (Migration migration : migrations) {
            try {
                jdbcTemplate.execute(migration.toAlterSql());
                log.info("迁移成功: {}.{} 列已添加", migration.tableName(), migration.columnName());
            } catch (Exception e) {
                if (isDuplicateColumn(e)) {
                    log.info("迁移跳过: {}.{} 列已存在", migration.tableName(), migration.columnName());
                } else {
                    log.error("迁移失败: {}.{} - {}", migration.tableName(), migration.columnName(), e.getMessage());
                }
            }
        }
    }

    protected void runNullBackfill(List<String> tables) {
        for (String table : tables) {
            try {
                int updated = jdbcTemplate.update("UPDATE \"" + table + "\" SET \"session_auth\" = 0 WHERE \"session_auth\" IS NULL");
                if (updated > 0) {
                    log.info("回填成功: {}.session_auth 已将 {} 行 NULL 更新为 0", table, updated);
                }
            } catch (Exception e) {
                log.warn("回填跳过: {}.session_auth 回填失败 - {}", table, e.getMessage());
            }
        }
    }
}
