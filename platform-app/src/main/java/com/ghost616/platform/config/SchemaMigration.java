package com.ghost616.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class SchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private record Migration(String tableName, String columnName, String columnType, String defaultValue) {
        String toAlterSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE \"").append(tableName).append("\" ADD COLUMN \"").append(columnName).append("\" ").append(columnType);
            if (defaultValue != null && !defaultValue.isEmpty()) {
                sql.append(" DEFAULT ").append(defaultValue);
            }
            return sql.toString();
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始执行数据库 Schema 迁移...");

        List<Migration> migrations = List.of(
                new Migration("tool_config", "auth_config", "TEXT", null),
                new Migration("session_variable", "update_time", "TIMESTAMP", null),
                new Migration("session_variable", "deleted", "INTEGER", "0"),
                new Migration("agent_config", "recent_message_count", "INTEGER", "10"),
                new Migration("message", "tool_result", "TEXT", null),
                new Migration("session", "parent_session_id", "BIGINT", null),
                new Migration("session", "is_child", "TINYINT(1)", "0"),
                new Migration("session", "description", "VARCHAR(500)", null),
                new Migration("session", "thinking", "TINYINT(1)", null),
                new Migration("session_tool", "session_auth", "INT", "0"),
                new Migration("agent_tool", "session_auth", "INT", "0"),
                new Migration("agent_skill", "session_auth", "INT", "0"),
                new Migration("session_skill", "session_auth", "INT", "0")
        );

        for (Migration migration : migrations) {
            try {
                jdbcTemplate.execute(migration.toAlterSql());
                log.info("迁移成功: {}.{} 列已添加", migration.tableName(), migration.columnName());
            } catch (Exception e) {
                String msg = e.getMessage();
                if (e instanceof SQLException && msg != null && msg.contains("duplicate column name")) {
                    log.info("迁移跳过: {}.{} 列已存在", migration.tableName(), migration.columnName());
                } else {
                    log.error("迁移失败: {}.{} - {}", migration.tableName(), migration.columnName(), msg);
                }
            }
        }

        List<String> nullBackfillTables = List.of(
                "session_tool", "agent_tool", "agent_skill", "session_skill"
        );
        for (String table : nullBackfillTables) {
            try {
                int updated = jdbcTemplate.update("UPDATE \"" + table + "\" SET \"session_auth\" = 0 WHERE \"session_auth\" IS NULL");
                if (updated > 0) {
                    log.info("回填成功: {}.session_auth 已将 {} 行 NULL 更新为 0", table, updated);
                }
            } catch (Exception e) {
                log.warn("回填跳过: {}.session_auth 回填失败 - {}", table, e.getMessage());
            }
        }

        log.info("数据库 Schema 迁移完成");
    }
}
