package com.ghost616.platform.migration;

import com.ghost616.platform.config.SchemaMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationArguments applicationArguments;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private SchemaMigration createMigration() {
        return new SchemaMigration(jdbcTemplate);
    }

    @Test
    void nullBackfill_所有表执行正确SQL() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        createMigration().run(applicationArguments);

        verify(jdbcTemplate, times(4)).update(sqlCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();
        assertEquals(4, sqls.size());

        assertTrue(sqls.get(0).contains("\"session_tool\""));
        assertTrue(sqls.get(1).contains("\"agent_tool\""));
        assertTrue(sqls.get(2).contains("\"agent_skill\""));
        assertTrue(sqls.get(3).contains("\"session_skill\""));
        for (String sql : sqls) {
            assertTrue(sql.contains("SET \"session_auth\" = 0"));
            assertTrue(sql.contains("WHERE \"session_auth\" IS NULL"));
        }
    }

    @Test
    void nullBackfill_有数据回填时不抛异常() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(5);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
    }

    @Test
    void nullBackfill_update抛出异常时捕获并记录warn_不中断启动() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenThrow(new DataAccessException("mock db error") {});

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
    }

    @Test
    void nullBackfill_部分表异常不影响其他表回填() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(contains("session_tool"))).thenThrow(new DataAccessException("mock error") {});
        when(jdbcTemplate.update(contains("agent_tool"))).thenReturn(3);
        when(jdbcTemplate.update(contains("agent_skill"))).thenReturn(0);
        when(jdbcTemplate.update(contains("session_skill"))).thenReturn(1);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
        verify(jdbcTemplate, times(4)).update(anyString());
    }

    @Test
    void nullBackfill_无需回填时静默跳过() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
    }

    @Test
    void alterTable_所有迁移正常执行() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
        verify(jdbcTemplate, times(15)).execute(anyString());
    }

    @Test
    void alterTable_SQLException列已存在时跳过不中断() {
        doThrow(new RuntimeException(new SQLException("duplicate column name: auth_config")))
                .when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
        verify(jdbcTemplate, times(15)).execute(anyString());
    }

    @Test
    void alterTable_未知异常记录error日志不中断() {
        doThrow(new RuntimeException("connection lost"))
                .when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        assertDoesNotThrow(() -> createMigration().run(applicationArguments));
        verify(jdbcTemplate, times(15)).execute(anyString());
    }

    @Test
    void alterTable_带DEFAULT值的列生成正确SQL() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        createMigration().run(applicationArguments);

        verify(jdbcTemplate, times(15)).execute(sqlCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();

        assertTrue(sqls.stream().anyMatch(s -> s.contains("DEFAULT 0")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("DEFAULT 10")));
    }

    @Test
    void alterTable_不带DEFAULT值的列生成SQL不含DEFAULT关键字() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        createMigration().run(applicationArguments);

        verify(jdbcTemplate, times(15)).execute(sqlCaptor.capture());
        long defaultCount = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("DEFAULT")).count();
        assertTrue(defaultCount > 0 && defaultCount < 15);
    }

    @Test
    void migration_包含total_token_used列() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        createMigration().run(applicationArguments);

        verify(jdbcTemplate, times(15)).execute(sqlCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().stream()
                .anyMatch(s -> s.contains("total_token_used") && s.contains("BIGINT")));
    }

    @Test
    void migration_包含token_usage列() {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        createMigration().run(applicationArguments);

        verify(jdbcTemplate, times(15)).execute(sqlCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().stream()
                .anyMatch(s -> s.contains("token_usage") && s.contains("TEXT")));
    }
}
