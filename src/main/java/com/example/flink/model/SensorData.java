package com.example.flink.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 传感器数据模型 - 用于接收Kafka消息
 */
public class SensorData implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("sensor_value")
    private Double sensorValue;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("location")
    private String location;

    @JsonProperty("status")
    private String status;

    public SensorData() {
    }

    public SensorData(String templateId, String deviceId, Double sensorValue, 
                     LocalDateTime timestamp, String location, String status) {
        this.templateId = templateId;
        this.deviceId = deviceId;
        this.sensorValue = sensorValue;
        this.timestamp = timestamp;
        this.location = location;
        this.status = status;
    }

    // Getters and Setters
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

    public Double getSensorValue() {
        return sensorValue;
    }

    public void setSensorValue(Double sensorValue) {
        this.sensorValue = sensorValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "templateId='" + templateId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", sensorValue=" + sensorValue +
                ", timestamp=" + timestamp +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
} 