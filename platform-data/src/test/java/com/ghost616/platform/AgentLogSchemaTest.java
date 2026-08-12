package com.ghost616.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLogSchemaTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:agentlog_schema;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private String readResource(String name) throws IOException {
        try (InputStream in = AgentLogSchemaTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "resource not found on classpath: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void schemaSqlAgentLogHasSessionVariablesColumn() throws IOException {
        String sql = readResource("schema-message.sql");
        assertTrue(sql.contains("session_variables      TEXT"),
                "agent_log should contain session_variables TEXT column");
    }

    @Test
    void schemaSqlAgentLogHasConversationVariablesColumn() throws IOException {
        String sql = readResource("schema-message.sql");
        assertTrue(sql.contains("conversation_variables TEXT"),
                "agent_log should contain conversation_variables TEXT column");
    }

    @Test
    void schemaSqlDdlExecutesSuccessfullyOnH2() throws Exception {
        String sql = readResource("schema.sql");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            }
        }
    }

    @Test
    void agentLogTableContainsNewColumns() throws Exception {
        String sql = readResource("schema-message.sql");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            }
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_NAME = 'AGENT_LOG'")) {
                    boolean hasSession = false;
                    boolean hasConversation = false;
                    while (rs.next()) {
                        String columnName = rs.getString(1);
                        if ("session_variables".equalsIgnoreCase(columnName)) {
                            hasSession = true;
                        }
                        if ("conversation_variables".equalsIgnoreCase(columnName)) {
                            hasConversation = true;
                        }
                    }
                    assertTrue(hasSession, "agent_log table should have session_variables column");
                    assertTrue(hasConversation, "agent_log table should have conversation_variables column");
                }
            }
        }
    }

    @Test
    void agentLogRoundTripInsertSelectNewColumns() throws Exception {
        String sql = readResource("schema-message.sql");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO agent_log (id, session_id, log_type, session_variables, conversation_variables) " +
                            "VALUES (1, 100, 'TEST', ?, ?)")) {
                ps.setString(1, "{\"k\":\"v\"}");
                ps.setString(2, "{\"a\":1}");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT session_variables, conversation_variables FROM agent_log WHERE id = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "row should exist");
                    assertTrue("{\"k\":\"v\"}".equals(rs.getString("session_variables")),
                            "session_variables should round-trip");
                    assertTrue("{\"a\":1}".equals(rs.getString("conversation_variables")),
                            "conversation_variables should round-trip");
                }
            }
        }
    }
}
