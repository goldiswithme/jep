package com.cloudpower.scheduler.entity;

import com.cloudpower.scheduler.enums.ProtocolType;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 外部系统接口配置实体
 * External System Interface Configuration Entity
 */
@Entity
@Table(name = "external_interface_config")
public class ExternalInterfaceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "系统名称不能为空")
    @Column(name = "system_name", nullable = false, length = 100)
    private String systemName;

    @NotBlank(message = "接口名称不能为空")
    @Column(name = "interface_name", nullable = false, length = 100)
    private String interfaceName;

    @NotNull(message = "协议类型不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_type", nullable = false)
    private ProtocolType protocolType;

    @NotBlank(message = "接口地址不能为空")
    @Column(name = "interface_url", nullable = false, length = 500)
    private String interfaceUrl;

    @Column(name = "http_method", length = 10)
    private String httpMethod = "POST";

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "request_template", columnDefinition = "TEXT")
    private String requestTemplate;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;

    @Column(name = "retry_times")
    private Integer retryTimes = 3;

    @Column(name = "retry_interval_seconds")
    private Integer retryIntervalSeconds = 5;

    @Column(name = "auth_required")
    private Boolean authRequired = false;

    @Column(name = "auth_interface_id")
    private Long authInterfaceId;

    @Column(name = "token_cache_key", length = 100)
    private String tokenCacheKey;

    @Column(name = "token_expire_seconds")
    private Integer tokenExpireSeconds = 7200;

    @Column(name = "data_transform_script", columnDefinition = "TEXT")
    private String dataTransformScript;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    // Constructors
    public ExternalInterfaceConfig() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public String getInterfaceUrl() {
        return interfaceUrl;
    }

    public void setInterfaceUrl(String interfaceUrl) {
        this.interfaceUrl = interfaceUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getRequestTemplate() {
        return requestTemplate;
    }

    public void setRequestTemplate(String requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public Integer getRetryIntervalSeconds() {
        return retryIntervalSeconds;
    }

    public void setRetryIntervalSeconds(Integer retryIntervalSeconds) {
        this.retryIntervalSeconds = retryIntervalSeconds;
    }

    public Boolean getAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(Boolean authRequired) {
        this.authRequired = authRequired;
    }

    public Long getAuthInterfaceId() {
        return authInterfaceId;
    }

    public void setAuthInterfaceId(Long authInterfaceId) {
        this.authInterfaceId = authInterfaceId;
    }

    public String getTokenCacheKey() {
        return tokenCacheKey;
    }

    public void setTokenCacheKey(String tokenCacheKey) {
        this.tokenCacheKey = tokenCacheKey;
    }

    public Integer getTokenExpireSeconds() {
        return tokenExpireSeconds;
    }

    public void setTokenExpireSeconds(Integer tokenExpireSeconds) {
        this.tokenExpireSeconds = tokenExpireSeconds;
    }

    public String getDataTransformScript() {
        return dataTransformScript;
    }

    public void setDataTransformScript(String dataTransformScript) {
        this.dataTransformScript = dataTransformScript;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}