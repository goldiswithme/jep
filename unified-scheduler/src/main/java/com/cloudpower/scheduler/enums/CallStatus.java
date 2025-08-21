package com.cloudpower.scheduler.enums;

/**
 * 接口调用状态枚举
 * Interface Call Status Enum
 */
public enum CallStatus {
    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    TIMEOUT("TIMEOUT", "超时"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    CallStatus(String code, String description) {
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