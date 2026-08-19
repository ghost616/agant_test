package com.ghost616.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryConfigSchemaTest {

    private String readResource(String name) throws IOException {
        try (InputStream in = MemoryConfigSchemaTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "resource not found on classpath: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void schemaSqlAgentConfigHasMemoryColumns() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("memory_enabled          TINYINT(1) DEFAULT 0"),
                "agent_config should contain memory_enabled column with default 0");
        assertTrue(sql.contains("memory_group_count      INTEGER DEFAULT 30"),
                "agent_config should contain memory_group_count column with default 30");
        assertTrue(sql.contains("vector_model_id         BIGINT"),
                "agent_config should contain vector_model_id column");
    }

    @Test
    void schemaSqlAgentConfigHasSubSessionOpenModeColumn() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("sub_session_open_mode   VARCHAR(32) DEFAULT 'TOOL_CALL'"),
                "agent_config should contain sub_session_open_mode column with default TOOL_CALL");
    }

    @Test
    void schemaSqlSessionHasMemoryPointSequenceNumColumn() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("memory_point_sequence_num INTEGER"),
                "session should contain memory_point_sequence_num column");
    }

    @Test
    void schemaSqlSessionHasMemoryPromptColumn() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("memory_prompt     VARCHAR(500)"),
                "session should contain memory_prompt column");
    }

    @Test
    void schemaMigrationContainsMemoryEntries() throws IOException {
        Path source = Paths.get("src/main/java/com/ghost616/platform/config/PrimarySchemaMigration.java");
        assertTrue(Files.exists(source), "PrimarySchemaMigration source file should exist");
        String content = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(content.contains("new Migration(\"agent_config\", \"memory_enabled\", \"TINYINT(1)\", \"0\")"),
                "missing migration: agent_config.memory_enabled");
        assertTrue(content.contains("new Migration(\"agent_config\", \"memory_group_count\", \"INTEGER\", \"30\")"),
                "missing migration: agent_config.memory_group_count");
        assertTrue(content.contains("new Migration(\"agent_config\", \"vector_model_id\", \"BIGINT\", null)"),
                "missing migration: agent_config.vector_model_id");
        assertTrue(content.contains("new Migration(\"session\", \"memory_point_sequence_num\", \"INTEGER\", null)"),
                "missing migration: session.memory_point_sequence_num");
        assertTrue(content.contains("new Migration(\"session\", \"memory_prompt\", \"VARCHAR(500)\", null)"),
                "missing migration: session.memory_prompt");
    }
}
