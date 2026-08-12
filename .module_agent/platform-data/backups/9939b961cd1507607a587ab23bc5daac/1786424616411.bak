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

    private boolean isDuplicateColumn(Throwable e) {
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
                new Migration("session_skill", "session_auth", "INT", "0"),
                new Migration("session", "total_token_used", "BIGINT", null),
                new Migration("message", "token_usage", "TEXT", null),
                new Migration("message", "conversation_id", "VARCHAR(50)", null),
                new Migration("message", "rollback", "TINYINT(1)", "0"),
                new Migration("tool_config", "sub_tool_type", "VARCHAR(32)", null),
                new Migration("tool_config", "tool_script", "TEXT", null),
                new Migration("session", "is_evaluation", "TINYINT(1)", "0"),
                new Migration("session", "last_response_id", "VARCHAR(50)", null),
                new Migration("evaluation", "id", "BIGINT", null),
                new Migration("evaluation", "name", "VARCHAR(255)", null),
                new Migration("evaluation", "description", "TEXT", null),
                new Migration("evaluation", "benchmark_session_id", "BIGINT", null),
                new Migration("evaluation", "execution_count", "INTEGER", "0"),
                new Migration("evaluation", "model_id", "BIGINT", null),
                new Migration("evaluation", "create_time", "TIMESTAMP", null),
                new Migration("evaluation", "update_time", "TIMESTAMP", null),
                new Migration("evaluation", "deleted", "INTEGER", "0"),
                new Migration("evaluation_result", "id", "BIGINT", null),
                new Migration("evaluation_result", "evaluation_id", "BIGINT", null),
                new Migration("evaluation_result", "evaluation_session_id", "BIGINT", null),
                new Migration("evaluation_result", "result", "TEXT", null),
                new Migration("evaluation_result", "execution_status", "VARCHAR(32)", "'PENDING'"),
                new Migration("evaluation_result", "model_id", "BIGINT", null),
                new Migration("evaluation_result", "final_score", "INTEGER", null),
                new Migration("evaluation_result", "create_time", "TIMESTAMP", null),
                new Migration("evaluation_result", "update_time", "TIMESTAMP", null),
                new Migration("evaluation_result", "deleted", "INTEGER", "0"),
                new Migration("agent_evaluation", "id", "BIGINT", null),
                new Migration("agent_evaluation", "name", "VARCHAR(255)", null),
                new Migration("agent_evaluation", "description", "TEXT", null),
                new Migration("agent_evaluation", "agent_id", "BIGINT", null),
                new Migration("agent_evaluation", "create_time", "TIMESTAMP", null),
                new Migration("agent_evaluation", "update_time", "TIMESTAMP", null),
                new Migration("agent_evaluation", "deleted", "INTEGER", "0"),
                new Migration("evaluation", "agent_eval_id", "BIGINT", null),
                new Migration("evaluation", "agent_id", "BIGINT", null),
                new Migration("evaluation", "execution_type", "VARCHAR(32)", "'BACKGROUND'"),
                new Migration("model_config", "request_type", "VARCHAR(32)", null),
                new Migration("model_config", "model_type", "VARCHAR(32)", "'LLM'"),
                new Migration("message_tool_call", "type", "VARCHAR(32)", "'function'"),
                new Migration("message_tool_call", "web_search_call", "TEXT", null),
                new Migration("message_tool_call", "custom_tool_call", "TEXT", null),
                new Migration("knowledge_base", "id", "BIGINT", null),
                new Migration("knowledge_base", "name", "VARCHAR(255)", null),
                new Migration("knowledge_base", "description", "TEXT", null),
                new Migration("knowledge_base", "status", "VARCHAR(32)", null),
                new Migration("knowledge_base", "vector_model_id", "BIGINT", null),
                new Migration("knowledge_base", "es_index", "VARCHAR(255)", null),
                new Migration("knowledge_base", "rebuilding", "TINYINT(1)", "0"),
                new Migration("knowledge_base", "create_time", "TIMESTAMP", null),
                new Migration("knowledge_base", "update_time", "TIMESTAMP", null),
                new Migration("knowledge_base", "deleted", "INTEGER", "0"),
                new Migration("knowledge_file", "id", "BIGINT", null),
                new Migration("knowledge_file", "file_name", "VARCHAR(255)", null),
                new Migration("knowledge_file", "file_description", "TEXT", null),
                new Migration("knowledge_file", "knowledge_base_id", "BIGINT", null),
                new Migration("knowledge_file", "file_size", "BIGINT", null),
                new Migration("knowledge_file", "line_count", "INTEGER", null),
                new Migration("knowledge_file", "status", "VARCHAR(32)", null),
                new Migration("knowledge_file", "publish_status", "VARCHAR(32)", "'UNPUBLISHED'"),
                new Migration("knowledge_file", "file_content", "LONGTEXT", null),
                new Migration("knowledge_file", "create_time", "TIMESTAMP", null),
                new Migration("knowledge_file", "update_time", "TIMESTAMP", null),
                new Migration("knowledge_file", "deleted", "INTEGER", "0"),
                new Migration("agent_knowledge_base", "id", "BIGINT", null),
                new Migration("agent_knowledge_base", "agent_id", "BIGINT", null),
                new Migration("agent_knowledge_base", "knowledge_base_id", "BIGINT", null),
                new Migration("agent_log", "session_variables", "TEXT", null),
                new Migration("agent_log", "conversation_variables", "TEXT", null)
        );

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
