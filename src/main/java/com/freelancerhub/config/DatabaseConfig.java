package com.freelancerhub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DatabaseConfig.java
 * Manually configures the MySQL connection pool using HikariCP.
 * HikariCP is the fastest Java connection pool — Spring Boot includes it by default.
 * This class gives us full control over connection settings.
 */
@Configuration
public class DatabaseConfig {

    // These values are read from application.properties (or Railway env vars)
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String dbDriver;

    /**
     * Creates and configures the DataSource bean.
     * Spring uses this bean whenever it needs to talk to MySQL.
     * @Primary tells Spring to use this DataSource if there are multiple defined.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Core connection settings
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName(dbDriver);

        // Pool settings — controls how many DB connections are kept open
        config.setMaximumPoolSize(10);        // max 10 simultaneous connections
        config.setMinimumIdle(2);             // always keep 2 connections ready
        config.setConnectionTimeout(30000);   // wait up to 30s for a connection
        config.setIdleTimeout(600000);        // close idle connections after 10 min
        config.setMaxLifetime(1800000);       // recycle connections every 30 min

        // Pool name — shows in logs for easier debugging
        config.setPoolName("FreelancerHubPool");

        // Test query — verifies the connection is alive before using it
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }
}