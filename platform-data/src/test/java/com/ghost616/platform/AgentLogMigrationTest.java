package com.ghost616.platform;

import com.ghost616.platform.config.SchemaMigration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLogMigrationTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:agentlog_migration;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(JDBC_URL);
        ds.setUsername("sa");
        ds.setPassword("");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS agent_log");
    }

    private String readResource(String name) throws IOException {
        try (InputStream in = AgentLogMigrationTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "resource not found on classpath: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> agentLogColumns() {
        return jdbc.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE UPPER(TABLE_NAME) = 'AGENT_LOG'")
                .stream()
                .map(row -> String.valueOf(row.get("COLUMN_NAME")).toLowerCase())
                .toList();
    }

    private void createOldAgentLogTable() {
        jdbc.execute("CREATE TABLE agent_log (" +
                "id BIGINT PRIMARY KEY, session_id BIGINT, conversation_id VARCHAR(50), " +
                "log_type VARCHAR(64), log_level VARCHAR(32), log_data TEXT, " +
                "create_time TIMESTAMP, update_time TIMESTAMP, deleted INTEGER DEFAULT 0)");
    }

    @Test
    void schemaMigrationSourceContainsAgentLogMigrations() throws IOException {
        Path path = Paths.get("src/main/java/com/ghost616/platform/config/SchemaMigration.java");
        assertTrue(Files.exists(path), "SchemaMigration source file should exist");
        String content = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue(content.contains("new Migration(\"agent_log\", \"session_variables\", \"TEXT\", null)"),
                "missing migration: agent_log.session_variables");
        assertTrue(content.contains("new Migration(\"agent_log\", \"conversation_variables\", \"TEXT\", null)"),
                "missing migration: agent_log.conversation_variables");
    }

    @Test
    void oldAgentLogTableGetsNewColumnsAfterMigration() {
        createOldAgentLogTable();

        new SchemaMigration(jdbc).run(null);

        List<String> columns = agentLogColumns();
        assertTrue(columns.contains("session_variables"),
                "old agent_log table should gain session_variables after migration");
        assertTrue(columns.contains("conversation_variables"),
                "old agent_log table should gain conversation_variables after migration");
    }

    @Test
    void freshAgentLogTableWithColumnsMigrationSkipsGracefully() {
        jdbc.execute("CREATE TABLE agent_log (" +
                "id BIGINT PRIMARY KEY, session_id BIGINT, conversation_id VARCHAR(50), " +
                "log_type VARCHAR(64), log_level VARCHAR(32), log_data TEXT, " +
                "session_variables TEXT, conversation_variables TEXT, " +
                "create_time TIMESTAMP, update_time TIMESTAMP, deleted INTEGER DEFAULT 0)");

        assertDoesNotThrow(() -> new SchemaMigration(jdbc).run(null));

        List<String> columns = agentLogColumns();
        assertTrue(columns.contains("session_variables"));
        assertTrue(columns.contains("conversation_variables"));
    }

    @Test
    void migratedOldTableCanStoreNewColumns() {
        createOldAgentLogTable();

        new SchemaMigration(jdbc).run(null);

        jdbc.update("INSERT INTO agent_log (id, session_id, log_type, session_variables, conversation_variables) " +
                "VALUES (1, 100, 'TEST', ?, ?)", "{\"k\":\"v\"}", "{\"a\":1}");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT session_variables, conversation_variables FROM agent_log WHERE id = 1");
        assertTrue("{\"k\":\"v\"}".equals(String.valueOf(row.get("SESSION_VARIABLES"))),
                "session_variables should round-trip after migration");
        assertTrue("{\"a\":1}".equals(String.valueOf(row.get("CONVERSATION_VARIABLES"))),
                "conversation_variables should round-trip after migration");
    }
}
