package com.devtinder.config;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    @Test
    void testNeonPostgreSQLUriParsing() {
        String neonUri = "postgresql://neondb_owner:npg_secret123@ep-cool-fog-123456.aws.neon.tech/neondb?sslmode=require";
        HikariConfig config = DataSourceConfig.buildHikariConfig(neonUri, null, null, "org.postgresql.Driver");

        assertEquals("jdbc:postgresql://ep-cool-fog-123456.aws.neon.tech:5432/neondb?sslmode=require", config.getJdbcUrl());
        assertEquals("neondb_owner", config.getUsername());
        assertEquals("npg_secret123", config.getPassword());
    }

    @Test
    void testRenderPostgresUriWithoutPort() {
        String renderUri = "postgres://dbuser:mypass@dpg-abc123xyz.oregon-postgres.render.com/mydb";
        HikariConfig config = DataSourceConfig.buildHikariConfig(renderUri, null, null, "org.postgresql.Driver");

        assertEquals("jdbc:postgresql://dpg-abc123xyz.oregon-postgres.render.com:5432/mydb?sslmode=require", config.getJdbcUrl());
        assertEquals("dbuser", config.getUsername());
        assertEquals("mypass", config.getPassword());
    }

    @Test
    void testStandardJdbcUrlPreserved() {
        String jdbcUrl = "jdbc:postgresql://ep-xyz.aws.neon.tech:5432/neondb?sslmode=require";
        HikariConfig config = DataSourceConfig.buildHikariConfig(jdbcUrl, "neondb_owner", "mypass", "org.postgresql.Driver");

        assertEquals("jdbc:postgresql://ep-xyz.aws.neon.tech:5432/neondb?sslmode=require", config.getJdbcUrl());
        assertEquals("neondb_owner", config.getUsername());
        assertEquals("mypass", config.getPassword());
    }

    @Test
    void testH2TestDatabasePreserved() {
        String h2Url = "jdbc:h2:mem:testdb;MODE=PostgreSQL";
        HikariConfig config = DataSourceConfig.buildHikariConfig(h2Url, "sa", "", "org.h2.Driver");

        assertEquals(h2Url, config.getJdbcUrl());
        assertEquals("sa", config.getUsername());
        assertEquals("", config.getPassword());
        assertEquals("org.h2.Driver", config.getDriverClassName());
    }
}
