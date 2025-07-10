package com.example.flink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 传感器统计数据实体 - 用于数据库存储
 */
@TableName("sensor_statistics")
public class SensorStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private String templateId;

    @TableField("device_id")
    private String deviceId;

    @TableField("data_count")
    private Long dataCount;

    @TableField("window_start")
    private LocalDateTime windowStart;

    @TableField("window_end")
    private LocalDateTime windowEnd;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    public SensorStatistics() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    public SensorStatistics(String templateId, String deviceId, Long dataCount,
                           LocalDateTime windowStart, LocalDateTime windowEnd) {
        this();
        this.templateId = templateId;
        this.deviceId = deviceId;
        this.dataCount = dataCount;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getDataCount() {
        return dataCount;
    }

    public void setDataCount(Long dataCount) {
        this.dataCount = dataCount;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return "SensorStatistics{" +
                "id=" + id +
                ", templateId='" + templateId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", dataCount=" + dataCount +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
} 