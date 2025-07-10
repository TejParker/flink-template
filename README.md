# Flink Template 项目

一个基于Apache Flink的流处理模板项目，集成Spring Boot配置管理，支持从Kafka消费数据，进行实时统计分析，并将结果写入MySQL或PostgreSQL数据库。

## 项目特性

- ✅ **Spring Boot集成**: 利用Spring Boot的依赖注入和配置管理能力
- ✅ **多数据库支持**: 支持MySQL和PostgreSQL，使用MyBatis Plus注解
- ✅ **Kafka集成**: 消费Kafka topic中的JSON数据
- ✅ **实时窗口统计**: 基于template_id和device_id分组统计
- ✅ **数据库Upsert**: 支持插入和更新操作，处理重复数据
- ✅ **环境配置**: 支持dev/test/prod多环境配置
- ✅ **完整打包流程**: 提供构建和部署脚本

## 项目结构

```
flink-template/
├── src/
│   ├── main/
│   │   ├── java/com/example/flink/
│   │   │   ├── FlinkStreamingApplication.java    # 主应用程序
│   │   │   ├── config/                           # 配置类
│   │   │   │   ├── FlinkProperties.java         # 配置属性类
│   │   │   │   └── DataSourceConfig.java        # 数据源配置
│   │   │   ├── model/                           # 数据模型
│   │   │   │   ├── SensorData.java              # Kafka消息模型
│   │   │   │   └── SensorStatistics.java        # 数据库实体（@TableName）
│   │   │   ├── service/                         # 服务类
│   │   │   │   └── DatabaseService.java         # 数据库服务
│   │   │   └── util/                            # 工具类
│   │   │       └── JsonUtils.java               # JSON工具
│   │   └── resources/
│   │       ├── application.yml                  # 配置文件
│   │       └── logback-spring.xml              # 日志配置
│   └── test/                                    # 测试代码
├── scripts/                                     # 脚本文件
│   ├── build.sh                                 # 构建脚本
│   ├── deploy.sh                                # 部署脚本
│   ├── init-mysql.sql                           # MySQL初始化脚本
│   └── init-postgresql.sql                      # PostgreSQL初始化脚本
├── pom.xml                                      # Maven配置
└── README.md                                    # 项目说明
```

## 快速开始

### 1. 环境准备

- Java 11+
- Maven 3.6+
- Apache Flink 1.17+
- Kafka 2.8+
- MySQL 8.0+ 或 PostgreSQL 12+

### 2. 数据库初始化

**MySQL:**
```bash
mysql -u root -p < scripts/init-mysql.sql
```

**PostgreSQL:**
```bash
psql -U postgres -d postgres -f scripts/init-postgresql.sql
```

### 3. 配置修改

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
# Kafka配置
kafka:
  bootstrap-servers: your-kafka-server:9092
  topic: your-topic-name

# 数据库配置
database:
  type: mysql  # 或 postgresql
  mysql:
    url: jdbc:mysql://your-db-server:3306/your-database
    username: your-username
    password: your-password
```

### 4. 构建项目

```bash
# 赋予执行权限
chmod +x scripts/build.sh

# 构建项目
./scripts/build.sh
```

### 5. 部署运行

```bash
# 赋予执行权限
chmod +x scripts/deploy.sh

# 提交到Flink集群
./scripts/deploy.sh submit --env prod --parallelism 4

# 查看作业状态
./scripts/deploy.sh list
```

## 配置说明

### Kafka消息格式

项目期望的Kafka消息格式为JSON：

```json
{
  "template_id": "template_001",
  "device_id": "device_001", 
  "sensor_value": 25.6,
  "timestamp": "2023-12-01 10:00:00",
  "location": "room_101",
  "status": "normal"
}
```

### 数据库表结构

```sql
CREATE TABLE sensor_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id VARCHAR(50) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    data_count BIGINT NOT NULL DEFAULT 0,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_device_window (template_id, device_id, window_start, window_end)
);
```

### 配置文件说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `kafka.bootstrap-servers` | Kafka服务器地址 | localhost:9092 |
| `kafka.topic` | 消费的Topic名称 | sensor_data |
| `database.type` | 数据库类型 | mysql |
| `flink.parallelism` | 并行度 | 2 |
| `flink.window.size` | 窗口大小(秒) | 60 |
| `flink.window.slide` | 滑动间隔(秒) | 30 |

## 业务逻辑

项目实现以下业务流程：

1. **数据消费**: 从Kafka topic消费JSON格式的传感器数据
2. **数据解析**: 将JSON数据反序列化为SensorData对象
3. **分组统计**: 按template_id和device_id分组，使用滑动窗口统计每个分组的数据条数
4. **数据存储**: 将统计结果写入数据库，支持插入和更新操作

## 脚本使用

### 构建脚本

```bash
./scripts/build.sh [选项]

选项:
  --skip-tests     跳过测试
  --skip-package   跳过打包  
  --clean-only     仅清理项目
  --help, -h       显示帮助信息
```

### 部署脚本

```bash
./scripts/deploy.sh [命令] [选项]

命令:
  submit      提交作业到Flink集群
  cancel      取消运行中的作业
  list        列出所有作业
  status      查看作业状态
  savepoint   创建保存点
  stop        停止作业

选项:
  --flink-home PATH     Flink安装目录
  --parallelism NUM     并行度
  --env ENVIRONMENT     环境(dev/test/prod)
  --job-id ID          作业ID
```

## 监控和运维

### 查看作业状态
```bash
./scripts/deploy.sh list
```

### 创建保存点
```bash
./scripts/deploy.sh savepoint --job-id <job-id>
```

### 从保存点恢复
```bash
./scripts/deploy.sh submit --savepoint-path <savepoint-path>
```

### 日志查看
日志文件位置：`logs/flink-template.log`

## 扩展开发

### 添加新的数据源

1. 在`config`包中创建新的配置类
2. 实现相应的连接器或Source
3. 在`FlinkStreamingApplication`中集成

### 自定义业务处理

1. 创建新的处理函数类
2. 在数据流管道中添加处理步骤
3. 根据需要修改数据模型

### 添加新的输出

1. 创建对应的Sink函数
2. 在`service`包中实现相关服务类
3. 配置相关连接信息

## 故障排查

### 常见问题

1. **连接数据库失败**
   - 检查数据库连接配置
   - 确认数据库服务是否启动
   - 验证用户权限

2. **Kafka连接超时**
   - 检查Kafka服务器地址
   - 确认Topic是否存在
   - 验证网络连通性

3. **作业提交失败**
   - 检查Flink集群状态
   - 验证JAR包是否正确构建
   - 查看Flink作业日志

### 性能调优

1. **调整并行度**: 根据数据量和集群资源调整
2. **优化窗口大小**: 平衡延迟和吞吐量
3. **数据库连接池**: 调整连接池大小
4. **检查点间隔**: 根据容错需求调整

## 许可证

本项目基于MIT许可证开源。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 联系方式

如有问题，请通过以下方式联系：
- 提交GitHub Issue
- 邮件联系：laplace1015@outlook.com 