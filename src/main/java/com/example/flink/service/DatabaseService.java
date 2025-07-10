package com.example.flink.service;

import com.example.flink.config.FlinkProperties;
import com.example.flink.model.SensorStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

/**
 * 数据库服务类 - 处理统计数据的CRUD操作
 */
@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlinkProperties flinkProperties;

    /**
     * 初始化数据库表结构
     */
    public void initializeDatabase() {
        String createTableSql = getCreateTableSql();
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.execute(createTableSql);
            logger.info("数据库表初始化完成");
            
        } catch (SQLException e) {
            logger.error("数据库表初始化失败", e);
            throw new RuntimeException("数据库表初始化失败", e);
        }
    }

    /**
     * 插入或更新统计数据（Upsert操作）
     */
    public void upsertStatistics(SensorStatistics statistics) {
        String upsertSql = getUpsertSql();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            
            setStatementParameters(statement, statistics);
            
            int rowsAffected = statement.executeUpdate();
            logger.debug("统计数据Upsert完成，影响行数: {}, 数据: {}", rowsAffected, statistics);
            
        } catch (SQLException e) {
            logger.error("统计数据Upsert失败: {}", statistics, e);
            throw new RuntimeException("统计数据Upsert失败", e);
        }
    }

    /**
     * 批量插入或更新统计数据
     */
    public void batchUpsertStatistics(Iterable<SensorStatistics> statisticsList) {
        String upsertSql = getUpsertSql();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            
            connection.setAutoCommit(false);
            
            int batchSize = 0;
            for (SensorStatistics statistics : statisticsList) {
                setStatementParameters(statement, statistics);
                statement.addBatch();
                batchSize++;
                
                if (batchSize % 100 == 0) {
                    statement.executeBatch();
                    statement.clearBatch();
                }
            }
            
            if (batchSize % 100 != 0) {
                statement.executeBatch();
            }
            
            connection.commit();
            logger.info("批量统计数据Upsert完成，总数: {}", batchSize);
            
        } catch (SQLException e) {
            logger.error("批量统计数据Upsert失败", e);
            throw new RuntimeException("批量统计数据Upsert失败", e);
        }
    }

    private String getCreateTableSql() {
        String databaseType = flinkProperties.getDatabase().getType();
        
        if ("mysql".equalsIgnoreCase(databaseType)) {
            return "CREATE TABLE IF NOT EXISTS sensor_statistics (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "template_id VARCHAR(50) NOT NULL," +
                    "device_id VARCHAR(50) NOT NULL," +
                    "data_count BIGINT NOT NULL DEFAULT 0," +
                    "window_start DATETIME NOT NULL," +
                    "window_end DATETIME NOT NULL," +
                    "created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_template_device_window (template_id, device_id, window_start, window_end)," +
                    "INDEX idx_template_device (template_id, device_id)," +
                    "INDEX idx_window_time (window_start, window_end)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            return "CREATE TABLE IF NOT EXISTS sensor_statistics (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "template_id VARCHAR(50) NOT NULL," +
                    "device_id VARCHAR(50) NOT NULL," +
                    "data_count BIGINT NOT NULL DEFAULT 0," +
                    "window_start TIMESTAMP NOT NULL," +
                    "window_end TIMESTAMP NOT NULL," +
                    "created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT uk_template_device_window UNIQUE (template_id, device_id, window_start, window_end)" +
                    "); " +
                    "CREATE INDEX IF NOT EXISTS idx_template_device ON sensor_statistics (template_id, device_id); " +
                    "CREATE INDEX IF NOT EXISTS idx_window_time ON sensor_statistics (window_start, window_end); " +
                    "CREATE OR REPLACE FUNCTION update_updated_time_column() " +
                    "RETURNS TRIGGER AS $$ " +
                    "BEGIN " +
                    "    NEW.updated_time = CURRENT_TIMESTAMP; " +
                    "    RETURN NEW; " +
                    "END; " +
                    "$$ language 'plpgsql'; " +
                    "DROP TRIGGER IF EXISTS update_sensor_statistics_updated_time ON sensor_statistics; " +
                    "CREATE TRIGGER update_sensor_statistics_updated_time " +
                    "    BEFORE UPDATE ON sensor_statistics " +
                    "    FOR EACH ROW " +
                    "    EXECUTE FUNCTION update_updated_time_column();";
        } else {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
    }

    private String getUpsertSql() {
        String databaseType = flinkProperties.getDatabase().getType();
        
        if ("mysql".equalsIgnoreCase(databaseType)) {
            return "INSERT INTO sensor_statistics (template_id, device_id, data_count, window_start, window_end, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "    data_count = VALUES(data_count), " +
                    "    updated_time = VALUES(updated_time)";
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            return "INSERT INTO sensor_statistics (template_id, device_id, data_count, window_start, window_end, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (template_id, device_id, window_start, window_end) " +
                    "DO UPDATE SET " +
                    "    data_count = EXCLUDED.data_count, " +
                    "    updated_time = EXCLUDED.updated_time";
        } else {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
    }

    private void setStatementParameters(PreparedStatement statement, SensorStatistics statistics) throws SQLException {
        statement.setString(1, statistics.getTemplateId());
        statement.setString(2, statistics.getDeviceId());
        statement.setLong(3, statistics.getDataCount());
        statement.setTimestamp(4, Timestamp.valueOf(statistics.getWindowStart()));
        statement.setTimestamp(5, Timestamp.valueOf(statistics.getWindowEnd()));
        statement.setTimestamp(6, Timestamp.valueOf(statistics.getCreatedTime()));
        statement.setTimestamp(7, Timestamp.valueOf(statistics.getUpdatedTime()));
    }
} 