package com.example.flink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Flink配置属性类
 */
@Component
@ConfigurationProperties(prefix = "")
public class FlinkProperties {

    private Kafka kafka = new Kafka();
    private Database database = new Database();
    private Flink flink = new Flink();

    public static class Kafka {
        private String bootstrapServers;
        private String topic;
        private String groupId;
        private String autoOffsetReset;
        private String keyDeserializer;
        private String valueDeserializer;

        // Getters and Setters
        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getAutoOffsetReset() {
            return autoOffsetReset;
        }

        public void setAutoOffsetReset(String autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
        }

        public String getKeyDeserializer() {
            return keyDeserializer;
        }

        public void setKeyDeserializer(String keyDeserializer) {
            this.keyDeserializer = keyDeserializer;
        }

        public String getValueDeserializer() {
            return valueDeserializer;
        }

        public void setValueDeserializer(String valueDeserializer) {
            this.valueDeserializer = valueDeserializer;
        }
    }

    public static class Database {
        private String type;
        private MySQL mysql = new MySQL();
        private PostgreSQL postgresql = new PostgreSQL();

        public static class MySQL {
            private String driverClassName;
            private String url;
            private String username;
            private String password;
            private Hikari hikari = new Hikari();

            // Getters and Setters
            public String getDriverClassName() {
                return driverClassName;
            }

            public void setDriverClassName(String driverClassName) {
                this.driverClassName = driverClassName;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public Hikari getHikari() {
                return hikari;
            }

            public void setHikari(Hikari hikari) {
                this.hikari = hikari;
            }
        }

        public static class PostgreSQL {
            private String driverClassName;
            private String url;
            private String username;
            private String password;
            private Hikari hikari = new Hikari();

            // Getters and Setters with same structure as MySQL
            public String getDriverClassName() {
                return driverClassName;
            }

            public void setDriverClassName(String driverClassName) {
                this.driverClassName = driverClassName;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public Hikari getHikari() {
                return hikari;
            }

            public void setHikari(Hikari hikari) {
                this.hikari = hikari;
            }
        }

        public static class Hikari {
            private int maximumPoolSize = 20;
            private int minimumIdle = 5;
            private long connectionTimeout = 30000;
            private long idleTimeout = 600000;
            private long maxLifetime = 1800000;

            // Getters and Setters
            public int getMaximumPoolSize() {
                return maximumPoolSize;
            }

            public void setMaximumPoolSize(int maximumPoolSize) {
                this.maximumPoolSize = maximumPoolSize;
            }

            public int getMinimumIdle() {
                return minimumIdle;
            }

            public void setMinimumIdle(int minimumIdle) {
                this.minimumIdle = minimumIdle;
            }

            public long getConnectionTimeout() {
                return connectionTimeout;
            }

            public void setConnectionTimeout(long connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
            }

            public long getIdleTimeout() {
                return idleTimeout;
            }

            public void setIdleTimeout(long idleTimeout) {
                this.idleTimeout = idleTimeout;
            }

            public long getMaxLifetime() {
                return maxLifetime;
            }

            public void setMaxLifetime(long maxLifetime) {
                this.maxLifetime = maxLifetime;
            }
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public MySQL getMysql() {
            return mysql;
        }

        public void setMysql(MySQL mysql) {
            this.mysql = mysql;
        }

        public PostgreSQL getPostgresql() {
            return postgresql;
        }

        public void setPostgresql(PostgreSQL postgresql) {
            this.postgresql = postgresql;
        }
    }

    public static class Flink {
        private int parallelism = 2;
        private Checkpoint checkpoint = new Checkpoint();
        private Window window = new Window();

        public static class Checkpoint {
            private long interval = 60000;
            private long timeout = 30000;
            private String mode = "EXACTLY_ONCE";

            // Getters and Setters
            public long getInterval() {
                return interval;
            }

            public void setInterval(long interval) {
                this.interval = interval;
            }

            public long getTimeout() {
                return timeout;
            }

            public void setTimeout(long timeout) {
                this.timeout = timeout;
            }

            public String getMode() {
                return mode;
            }

            public void setMode(String mode) {
                this.mode = mode;
            }
        }

        public static class Window {
            private int size = 60;
            private int slide = 30;

            // Getters and Setters
            public int getSize() {
                return size;
            }

            public void setSize(int size) {
                this.size = size;
            }

            public int getSlide() {
                return slide;
            }

            public void setSlide(int slide) {
                this.slide = slide;
            }
        }

        // Getters and Setters
        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }

        public Checkpoint getCheckpoint() {
            return checkpoint;
        }

        public void setCheckpoint(Checkpoint checkpoint) {
            this.checkpoint = checkpoint;
        }

        public Window getWindow() {
            return window;
        }

        public void setWindow(Window window) {
            this.window = window;
        }
    }

    // Main class Getters and Setters
    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Flink getFlink() {
        return flink;
    }

    public void setFlink(Flink flink) {
        this.flink = flink;
    }
} 