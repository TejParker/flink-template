import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonTimeTest {
    
    static class TestData {
        public String name;
        public LocalDateTime timestamp;
        
        public TestData() {}
        public TestData(String name, LocalDateTime timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }
    
    public static void main(String[] args) throws Exception {
        TestData data = new TestData("test", LocalDateTime.of(2023, 12, 1, 10, 0, 0));
        
        System.out.println("=== 不加 JavaTimeModule ===");
        ObjectMapper mapperWithoutModule = new ObjectMapper();
        try {
            String jsonWithoutModule = mapperWithoutModule.writeValueAsString(data);
            System.out.println("序列化结果: " + jsonWithoutModule);
        } catch (Exception e) {
            System.out.println("序列化失败: " + e.getClass().getSimpleName());
            System.out.println("错误信息: " + e.getMessage());
        }
        
        System.out.println("\n=== 加了 JavaTimeModule ===");
        ObjectMapper mapperWithModule = new ObjectMapper();
        mapperWithModule.registerModule(new JavaTimeModule());
        mapperWithModule.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String jsonWithModule = mapperWithModule.writeValueAsString(data);
        System.out.println("序列化结果: " + jsonWithModule);
        
        System.out.println("\n=== 反序列化对比测试 ===");
        String inputJson = "{\"name\":\"test\",\"timestamp\":\"2023-12-01T10:00:00\"}";
        System.out.println("输入JSON: " + inputJson);
        
        // 测试不加模块的反序列化
        try {
            TestData parsed1 = mapperWithoutModule.readValue(inputJson, TestData.class);
            System.out.println("不加模块: 解析成功 - " + parsed1.timestamp);
        } catch (Exception e) {
            System.out.println("不加模块: 解析失败 - " + e.getClass().getSimpleName());
        }
        
        // 测试加了模块的反序列化
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            TestData parsed2 = mapperWithModule.readValue(inputJson, TestData.class);
            // System.out.println("加了模块: 解析成功 - " + parsed2.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            System.out.println("加了模块: 解析成功 - " + parsed2.timestamp.format(formatter));
        } catch (Exception e) {
            System.out.println("加了模块: 解析失败 - " + e.getClass().getSimpleName());
        }
        
        System.out.println("\n=== 总结 ===");
        System.out.println("jackson-datatype-jsr310 的作用:");
        System.out.println("1. 让Jackson支持Java 8时间类型的序列化/反序列化");
        System.out.println("2. 避免 InvalidDefinitionException 异常");
        System.out.println("3. 支持标准的ISO时间格式字符串");
    }
} 