package com.cloudpower.scheduler.enums;

/**
 * 接口协议类型枚举
 * Interface Protocol Type Enum
 */
public enum ProtocolType {
    HTTP("HTTP", "HTTP协议"),
    HTTPS("HTTPS", "HTTPS协议"),
    WEBSOCKET("WebSocket", "WebSocket协议"),
    MQTT("MQTT", "MQTT协议"),
    WEBSERVICE("WebService", "Web服务协议");

    private final String code;
    private final String description;

    ProtocolType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}