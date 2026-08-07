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

class SchemaFilesTest {

    private String readResource(String name) throws IOException {
        try (InputStream in = SchemaFilesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "resource not found on classpath: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void schemaSqlKnowledgeBaseHasNewColumns() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("vector_model_id  BIGINT"), "knowledge_base should contain vector_model_id column");
        assertTrue(sql.contains("es_index         VARCHAR(255)"), "knowledge_base should contain es_index column");
        assertTrue(sql.contains("rebuilding       TINYINT(1) DEFAULT 0"), "knowledge_base should contain rebuilding column");
    }

    @Test
    void schemaSqlKnowledgeFileHasPublishStatusColumn() throws IOException {
        String sql = readResource("schema.sql");
        assertTrue(sql.contains("publish_status     VARCHAR(32) DEFAULT 'UNPUBLISHED'"),
                "knowledge_file should contain publish_status column with default UNPUBLISHED");
    }

    @Test
    void schemaMigrationContainsFourNewMigrationEntries() throws IOException {
        Path source = Paths.get("src/main/java/com/ghost616/platform/config/SchemaMigration.java");
        assertTrue(Files.exists(source), "SchemaMigration source file should exist");
        String content = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(content.contains("new Migration(\"knowledge_file\", \"publish_status\", \"VARCHAR(32)\", \"'UNPUBLISHED'\")"),
                "missing migration: knowledge_file.publish_status");
        assertTrue(content.contains("new Migration(\"knowledge_base\", \"vector_model_id\", \"BIGINT\", null)"),
                "missing migration: knowledge_base.vector_model_id");
        assertTrue(content.contains("new Migration(\"knowledge_base\", \"es_index\", \"VARCHAR(255)\", null)"),
                "missing migration: knowledge_base.es_index");
        assertTrue(content.contains("new Migration(\"knowledge_base\", \"rebuilding\", \"TINYINT(1)\", \"0\")"),
                "missing migration: knowledge_base.rebuilding");
    }
}
