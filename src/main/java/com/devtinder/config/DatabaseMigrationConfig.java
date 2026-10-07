package com.devtinder.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private final DataSource dataSource;

    public DatabaseMigrationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            log.info("Running database schema self-healing migrations...");
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE");
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS read_at TIMESTAMP WITH TIME ZONE");
            stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS project_pitch VARCHAR(600)");
            log.info("Database schema self-healing migrations executed successfully.");
        } catch (Exception e) {
            log.warn("Database schema self-healing warning: {}", e.getMessage());
        }
    }
}
