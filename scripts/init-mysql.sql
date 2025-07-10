-- MySQL数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS flink_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flink_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flink_prod DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE flink_db;

-- 创建传感器统计表
CREATE TABLE IF NOT EXISTS sensor_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_id VARCHAR(50) NOT NULL COMMENT '模板ID',
    device_id VARCHAR(50) NOT NULL COMMENT '设备ID', 
    data_count BIGINT NOT NULL DEFAULT 0 COMMENT '数据条数',
    window_start DATETIME NOT NULL COMMENT '窗口开始时间',
    window_end DATETIME NOT NULL COMMENT '窗口结束时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_device_window (template_id, device_id, window_start, window_end),
    INDEX idx_template_device (template_id, device_id),
    INDEX idx_window_time (window_start, window_end),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器统计数据表';

-- 插入示例数据（可选）
INSERT INTO sensor_statistics (template_id, device_id, data_count, window_start, window_end) 
VALUES 
    ('template_001', 'device_001', 100, '2023-12-01 10:00:00', '2023-12-01 10:01:00'),
    ('template_001', 'device_002', 150, '2023-12-01 10:00:00', '2023-12-01 10:01:00'),
    ('template_002', 'device_001', 80, '2023-12-01 10:00:00', '2023-12-01 10:01:00')
ON DUPLICATE KEY UPDATE 
    data_count = VALUES(data_count),
    updated_time = CURRENT_TIMESTAMP;

-- 创建用户（如果需要）
-- CREATE USER 'flink_user'@'%' IDENTIFIED BY 'flink_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON flink_db.* TO 'flink_user'@'%';
-- FLUSH PRIVILEGES;

-- 查看表结构
DESCRIBE sensor_statistics; 