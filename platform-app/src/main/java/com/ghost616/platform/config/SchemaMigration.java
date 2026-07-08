package com.ghost616.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        log.debug("开始执行数据库 Schema 迁移...");

        List<Migration> migrations = List.of(
                new Migration("tool_config", "auth_config", "TEXT", null),
                new Migration("session_variable", "update_time", "TIMESTAMP", null),
                new Migration("session_variable", "deleted", "INTEGER", "0"),
                new Migration("agent_config", "recent_message_count", "INTEGER", "10"),
                new Migration("message", "tool_result", "TEXT", null)
        );

        for (Migration migration : migrations) {
            try {
                if (columnExists(migration.tableName(), migration.columnName())) {
                    log.debug("迁移跳过: {}.{} 列已存在", migration.tableName(), migration.columnName());
                } else {
                    jdbcTemplate.execute(migration.toAlterSql());
                    log.debug("迁移成功: {}.{} 列已添加", migration.tableName(), migration.columnName());
                }
            } catch (Exception e) {
                log.error("迁移失败: {}.{} - {}", migration.tableName(), migration.columnName(), e.getMessage());
            }
        }

        log.debug("数据库 Schema 迁移完成");
    }

    private boolean columnExists(String tableName, String columnName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "PRAGMA table_info(\"" + tableName + "\")"
        );
        return rows.stream().anyMatch(row -> columnName.equals(row.get("name")));
    }
}
