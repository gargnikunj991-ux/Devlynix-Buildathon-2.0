package com.devtinder.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String configuredDriver;

    public static HikariConfig buildHikariConfig(String rawUrl, String username, String password, String driver) {
        HikariConfig config = new HikariConfig();

        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = "jdbc:postgresql://localhost:5432/devtinder";
        }

        String jdbcUrl = rawUrl;
        String resolvedUsername = username;
        String resolvedPassword = password;

        // 1. In-memory H2 (for tests)
        if (rawUrl.startsWith("jdbc:h2:")) {
            config.setJdbcUrl(rawUrl);
            config.setUsername(StringUtils.hasText(resolvedUsername) ? resolvedUsername : "sa");
            config.setPassword(resolvedPassword != null ? resolvedPassword : "");
            config.setDriverClassName("org.h2.Driver");
            return config;
        }

        // 2. Normalize URI formats: 'postgres://' or 'postgresql://' (Neon, Render, Railway defaults)
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                String normalizedUri = rawUrl.startsWith("postgres://")
                        ? "postgresql://" + rawUrl.substring("postgres://".length())
                        : rawUrl;

                URI uri = new URI(normalizedUri);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "neondb";
                String query = uri.getQuery();

                // Extract credentials from user info if not explicitly provided
                if (uri.getUserInfo() != null) {
                    String[] userParts = uri.getUserInfo().split(":", 2);
                    if (!StringUtils.hasText(resolvedUsername)) {
                        resolvedUsername = userParts[0];
                    }
                    if (userParts.length > 1 && !StringUtils.hasText(resolvedPassword)) {
                        resolvedPassword = userParts[1];
                    }
                }

                StringBuilder jdbcBuilder = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(dbName);

                if (StringUtils.hasText(query)) {
                    jdbcBuilder.append("?").append(query);
                } else if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
                    jdbcBuilder.append("?sslmode=require");
                }

                jdbcUrl = jdbcBuilder.toString();
            } catch (Exception e) {
                log.error("Failed to parse database URI: {}. Keeping raw URL.", e.getMessage());
            }
        }

        // 3. Ensure remote cloud databases (Neon / Render) require SSL
        if (jdbcUrl.startsWith("jdbc:postgresql://")
                && !jdbcUrl.contains("localhost")
                && !jdbcUrl.contains("127.0.0.1")
                && !jdbcUrl.contains("sslmode=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        // 4. Fallback defaults if still empty
        if (!StringUtils.hasText(resolvedUsername)) {
            resolvedUsername = "postgres";
        }
        if (resolvedPassword == null) {
            resolvedPassword = "postgres";
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(resolvedUsername);
        config.setPassword(resolvedPassword);
        config.setDriverClassName(StringUtils.hasText(driver) ? driver : "org.postgresql.Driver");

        // Cloud pool optimization (Neon / Render free tier)
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(300000);      // 5 minutes
        config.setMaxLifetime(600000);     // 10 minutes
        // Avoid crashing instantly if Neon is waking up from idle sleep
        config.setInitializationFailTimeout(-1);

        return config;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = buildHikariConfig(configuredUrl, configuredUsername, configuredPassword, configuredDriver);

        if (config.getJdbcUrl().contains("localhost:5432")) {
            log.warn("⚠️ Database URL is using localhost:5432. On Render, set SPRING_DATASOURCE_URL or DATABASE_URL to your Neon database!");
        }

        log.info("Connecting to DataSource: URL={}, Username={}",
                config.getJdbcUrl().replaceAll("password=[^&]*", "password=***"),
                config.getUsername());

        return new HikariDataSource(config);
    }
}
