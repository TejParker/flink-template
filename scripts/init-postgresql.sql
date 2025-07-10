-- PostgreSQL数据库初始化脚本

-- 创建数据库（需要在postgres数据库中执行）
-- CREATE DATABASE flink_db WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';
-- CREATE DATABASE flink_dev WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';
-- CREATE DATABASE flink_prod WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';

-- 连接到目标数据库
\c flink_db;

-- 创建传感器统计表
CREATE TABLE IF NOT EXISTS sensor_statistics (
    id BIGSERIAL PRIMARY KEY,
    template_id VARCHAR(50) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    data_count BIGINT NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_template_device_window UNIQUE (template_id, device_id, window_start, window_end)
);

-- 添加注释
COMMENT ON TABLE sensor_statistics IS '传感器统计数据表';
COMMENT ON COLUMN sensor_statistics.id IS '主键ID';
COMMENT ON COLUMN sensor_statistics.template_id IS '模板ID';
COMMENT ON COLUMN sensor_statistics.device_id IS '设备ID';
COMMENT ON COLUMN sensor_statistics.data_count IS '数据条数';
COMMENT ON COLUMN sensor_statistics.window_start IS '窗口开始时间';
COMMENT ON COLUMN sensor_statistics.window_end IS '窗口结束时间';
COMMENT ON COLUMN sensor_statistics.created_time IS '创建时间';
COMMENT ON COLUMN sensor_statistics.updated_time IS '更新时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_template_device ON sensor_statistics (template_id, device_id);
CREATE INDEX IF NOT EXISTS idx_window_time ON sensor_statistics (window_start, window_end);
CREATE INDEX IF NOT EXISTS idx_created_time ON sensor_statistics (created_time);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_time_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 创建触发器
DROP TRIGGER IF EXISTS update_sensor_statistics_updated_time ON sensor_statistics;
CREATE TRIGGER update_sensor_statistics_updated_time
    BEFORE UPDATE ON sensor_statistics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_time_column();

-- 插入示例数据（可选）
INSERT INTO sensor_statistics (template_id, device_id, data_count, window_start, window_end) 
VALUES 
    ('template_001', 'device_001', 100, '2023-12-01 10:00:00', '2023-12-01 10:01:00'),
    ('template_001', 'device_002', 150, '2023-12-01 10:00:00', '2023-12-01 10:01:00'),
    ('template_002', 'device_001', 80, '2023-12-01 10:00:00', '2023-12-01 10:01:00')
ON CONFLICT (template_id, device_id, window_start, window_end)
DO UPDATE SET
    data_count = EXCLUDED.data_count,
    updated_time = CURRENT_TIMESTAMP;

-- 创建用户（如果需要）
-- CREATE USER flink_user WITH PASSWORD 'flink_password';
-- GRANT CONNECT ON DATABASE flink_db TO flink_user;
-- GRANT USAGE ON SCHEMA public TO flink_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO flink_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO flink_user;

-- 查看表结构
\d sensor_statistics; 