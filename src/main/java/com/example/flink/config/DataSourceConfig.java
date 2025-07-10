package com.example.flink.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据源配置类 - 支持MySQL和PostgreSQL
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    private FlinkProperties flinkProperties;

    @Bean
    public DataSource dataSource() {
        String databaseType = flinkProperties.getDatabase().getType();
        
        logger.info("初始化数据源，数据库类型: {}", databaseType);

        HikariConfig config = new HikariConfig();

        if ("mysql".equalsIgnoreCase(databaseType)) {
            FlinkProperties.Database.MySQL mysql = flinkProperties.getDatabase().getMysql();
            config.setDriverClassName(mysql.getDriverClassName());
            config.setJdbcUrl(mysql.getUrl());
            config.setUsername(mysql.getUsername());
            config.setPassword(mysql.getPassword());
            
            // MySQL 特定配置
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            // Hikari连接池配置
            setHikariConfig(config, mysql.getHikari());
            
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            FlinkProperties.Database.PostgreSQL postgresql = flinkProperties.getDatabase().getPostgresql();
            config.setDriverClassName(postgresql.getDriverClassName());
            config.setJdbcUrl(postgresql.getUrl());
            config.setUsername(postgresql.getUsername());
            config.setPassword(postgresql.getPassword());
            
            // PostgreSQL 特定配置
            config.addDataSourceProperty("stringtype", "unspecified");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("ApplicationName", "Flink Template Application");
            
            // Hikari连接池配置
            setHikariConfig(config, postgresql.getHikari());
            
        } else {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }

        config.setPoolName("FlinkTemplateHikariCP");
        
        logger.info("数据源配置完成: {}", config.getJdbcUrl());
        return new HikariDataSource(config);
    }

    private void setHikariConfig(HikariConfig config, FlinkProperties.Database.Hikari hikariProps) {
        config.setMaximumPoolSize(hikariProps.getMaximumPoolSize());
        config.setMinimumIdle(hikariProps.getMinimumIdle());
        config.setConnectionTimeout(hikariProps.getConnectionTimeout());
        config.setIdleTimeout(hikariProps.getIdleTimeout());
        config.setMaxLifetime(hikariProps.getMaxLifetime());
        config.setLeakDetectionThreshold(60000);
    }
} 