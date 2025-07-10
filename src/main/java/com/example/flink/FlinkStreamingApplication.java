package com.example.flink;

import com.example.flink.config.FlinkProperties;
import com.example.flink.model.SensorData;
import com.example.flink.model.SensorStatistics;
import com.example.flink.service.DatabaseService;
import com.example.flink.util.JsonUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Flink流处理应用程序主类
 */
@SpringBootApplication
public class FlinkStreamingApplication {

    private static final Logger logger = LoggerFactory.getLogger(FlinkStreamingApplication.class);

    public static void main(String[] args) throws Exception {
        // 启动Spring Boot应用上下文
        ConfigurableApplicationContext context = SpringApplication.run(FlinkStreamingApplication.class, args);
        
        // 获取配置
        FlinkProperties flinkProperties = context.getBean(FlinkProperties.class);
        DatabaseService databaseService = context.getBean(DatabaseService.class);
        
        // 初始化数据库
        databaseService.initializeDatabase();
        
        logger.info("开始启动Flink流处理应用程序");
        
        // 创建Flink执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 配置Flink环境
        configureFlinkEnvironment(env, flinkProperties);
        
        // 创建Kafka数据源
        KafkaSource<String> kafkaSource = createKafkaSource(flinkProperties);
        
        // 构建数据处理管道
        buildDataPipeline(env, kafkaSource, flinkProperties, context);
        
        // 执行作业
        logger.info("启动Flink作业...");
        env.execute("Sensor Data Processing Job");
    }

    private static void configureFlinkEnvironment(StreamExecutionEnvironment env, FlinkProperties flinkProperties) {
        // 设置并行度
        env.setParallelism(flinkProperties.getFlink().getParallelism());
        
        // 启用检查点
        env.enableCheckpointing(flinkProperties.getFlink().getCheckpoint().getInterval());
        
        // 配置检查点
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        checkpointConfig.setMinPauseBetweenCheckpoints(5000);
        checkpointConfig.setCheckpointTimeout(flinkProperties.getFlink().getCheckpoint().getTimeout());
        checkpointConfig.setTolerableCheckpointFailureNumber(3);
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        
        logger.info("Flink环境配置完成 - 并行度: {}, 检查点间隔: {}ms", 
                   flinkProperties.getFlink().getParallelism(),
                   flinkProperties.getFlink().getCheckpoint().getInterval());
    }

    private static KafkaSource<String> createKafkaSource(FlinkProperties flinkProperties) {
        FlinkProperties.Kafka kafkaConfig = flinkProperties.getKafka();
        
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaConfig.getBootstrapServers())
                .setTopics(kafkaConfig.getTopic())
                .setGroupId(kafkaConfig.getGroupId())
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
        
        logger.info("Kafka数据源创建完成 - 服务器: {}, Topic: {}, 消费组: {}", 
                   kafkaConfig.getBootstrapServers(),
                   kafkaConfig.getTopic(),
                   kafkaConfig.getGroupId());
        
        return kafkaSource;
    }

    private static void buildDataPipeline(StreamExecutionEnvironment env, 
                                        KafkaSource<String> kafkaSource,
                                        FlinkProperties flinkProperties,
                                        ConfigurableApplicationContext context) {
        
        // 从Kafka读取数据
        DataStream<String> kafkaStream = env.fromSource(kafkaSource, 
                WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                        .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis()),
                "Kafka Source");
        
        // 解析JSON数据
        SingleOutputStreamOperator<SensorData> sensorDataStream = kafkaStream
                .map(new JsonToSensorDataMapper())
                .filter(data -> data != null)
                .name("Parse JSON");
        
        // 分配水印（基于事件时间）
        SingleOutputStreamOperator<SensorData> watermarkedStream = sensorDataStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<SensorData>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                                .withTimestampAssigner((element, recordTimestamp) ->
                                        element.getTimestamp() != null ? 
                                        element.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli() :
                                        // element.getTimestamp().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli() :
                                                System.currentTimeMillis()));
        
        // 按template_id和device_id分组，使用滑动窗口进行聚合
        SingleOutputStreamOperator<SensorStatistics> statisticsStream = watermarkedStream
                .map(new SensorDataToKeyMapper())
                .keyBy(tuple -> tuple.f0) // 按组合键分组
                .window(SlidingEventTimeWindows.of(
                        Time.seconds(flinkProperties.getFlink().getWindow().getSize()),
                        Time.seconds(flinkProperties.getFlink().getWindow().getSlide())))
                .reduce(new DataCountReducer(), new StatisticsWindowFunction())
                .name("Window Aggregation");
        
        // 写入数据库
        statisticsStream.addSink(new DatabaseSinkFunction(context))
                .name("Database Sink");
        
        logger.info("数据处理管道构建完成");
    }

    /**
     * JSON字符串转SensorData的映射函数
     */
    public static class JsonToSensorDataMapper implements MapFunction<String, SensorData> {
        @Override
        public SensorData map(String jsonString) throws Exception {
            try {
                return JsonUtils.fromJson(jsonString, SensorData.class);
            } catch (Exception e) {
                logger.warn("解析JSON失败: {}", jsonString, e);
                return null;
            }
        }
    }

    /**
     * SensorData转换为分组键的映射函数
     */
    public static class SensorDataToKeyMapper implements MapFunction<SensorData, Tuple3<String, SensorData, Long>> {
        @Override
        public Tuple3<String, SensorData, Long> map(SensorData sensorData) throws Exception {
            String key = sensorData.getTemplateId() + "_" + sensorData.getDeviceId();
            return new Tuple3<>(key, sensorData, 1L);
        }
    }

    /**
     * 数据计数聚合函数
     */
    public static class DataCountReducer implements ReduceFunction<Tuple3<String, SensorData, Long>> {
        @Override
        public Tuple3<String, SensorData, Long> reduce(Tuple3<String, SensorData, Long> value1,
                                                       Tuple3<String, SensorData, Long> value2) throws Exception {
            return new Tuple3<>(value1.f0, value1.f1, value1.f2 + value2.f2);
        }
    }

    /**
     * 窗口统计函数
     */
    public static class StatisticsWindowFunction implements 
            org.apache.flink.streaming.api.functions.windowing.WindowFunction<
                    Tuple3<String, SensorData, Long>, SensorStatistics, String,
                    org.apache.flink.streaming.api.windowing.windows.TimeWindow> {
        
        @Override
        public void apply(String key, 
                         org.apache.flink.streaming.api.windowing.windows.TimeWindow window,
                         Iterable<Tuple3<String, SensorData, Long>> input,
                         org.apache.flink.util.Collector<SensorStatistics> out) throws Exception {
            
            Tuple3<String, SensorData, Long> result = input.iterator().next();
            SensorData sampleData = result.f1;
            Long count = result.f2;
            
            LocalDateTime windowStart = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(window.getStart()), ZoneOffset.UTC);
            LocalDateTime windowEnd = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(window.getEnd()), ZoneOffset.UTC);
            
            SensorStatistics statistics = new SensorStatistics(
                    sampleData.getTemplateId(),
                    sampleData.getDeviceId(),
                    count,
                    windowStart,
                    windowEnd
            );
            
            out.collect(statistics);
        }
    }

    /**
     * 数据库写入Sink函数
     */
    public static class DatabaseSinkFunction extends RichSinkFunction<SensorStatistics> {
        
        private DatabaseService databaseService;
        private ConfigurableApplicationContext context;
        
        public DatabaseSinkFunction(ConfigurableApplicationContext context) {
            this.context = context;
        }
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            this.databaseService = context.getBean(DatabaseService.class);
        }
        
        @Override
        public void invoke(SensorStatistics statistics, Context context) throws Exception {
            try {
                statistics.setUpdatedTime(LocalDateTime.now());
                databaseService.upsertStatistics(statistics);
                logger.debug("统计数据写入成功: {}", statistics);
            } catch (Exception e) {
                logger.error("统计数据写入失败: {}", statistics, e);
                throw e;
            }
        }
    }
} 