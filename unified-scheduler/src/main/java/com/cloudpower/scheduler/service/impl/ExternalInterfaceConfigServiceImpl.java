package com.cloudpower.scheduler.service.impl;

import com.cloudpower.scheduler.entity.ExternalInterfaceConfig;
import com.cloudpower.scheduler.repository.ExternalInterfaceConfigRepository;
import com.cloudpower.scheduler.service.ExternalInterfaceConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 外部接口配置服务实现
 * External Interface Configuration Service Implementation
 */
@Service
@Transactional
public class ExternalInterfaceConfigServiceImpl implements ExternalInterfaceConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalInterfaceConfigServiceImpl.class);

    @Autowired
    private ExternalInterfaceConfigRepository configRepository;

    @Override
    public ExternalInterfaceConfig save(ExternalInterfaceConfig config) {
        logger.info("Saving interface configuration for system: {}, interface: {}", 
                   config.getSystemName(), config.getInterfaceName());
        
        // 验证配置
        Map<String, String> validationErrors = validateConfig(config);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Configuration validation failed: " + validationErrors);
        }
        
        return configRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalInterfaceConfig> findById(Long id) {
        return configRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalInterfaceConfig> findBySystemAndInterface(String systemName, String interfaceName) {
        return configRepository.findBySystemNameAndInterfaceNameAndEnabledTrue(systemName, interfaceName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalInterfaceConfig> findEnabledBySystem(String systemName) {
        return configRepository.findBySystemNameAndEnabledTrue(systemName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalInterfaceConfig> findAllEnabled() {
        return configRepository.findAll().stream()
                .filter(ExternalInterfaceConfig::getEnabled)
                .toList();
    }

    @Override
    public ExternalInterfaceConfig update(ExternalInterfaceConfig config) {
        logger.info("Updating interface configuration ID: {}", config.getId());
        
        Optional<ExternalInterfaceConfig> existing = configRepository.findById(config.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Interface configuration not found: " + config.getId());
        }
        
        // 验证配置
        Map<String, String> validationErrors = validateConfig(config);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Configuration validation failed: " + validationErrors);
        }
        
        return configRepository.save(config);
    }

    @Override
    public void toggleEnabled(Long id, boolean enabled) {
        logger.info("Toggling interface configuration ID: {} to enabled: {}", id, enabled);
        
        Optional<ExternalInterfaceConfig> config = configRepository.findById(id);
        if (config.isPresent()) {
            ExternalInterfaceConfig conf = config.get();
            conf.setEnabled(enabled);
            configRepository.save(conf);
        } else {
            throw new IllegalArgumentException("Interface configuration not found: " + id);
        }
    }

    @Override
    public void deleteById(Long id) {
        logger.info("Deleting interface configuration ID: {}", id);
        
        if (!configRepository.existsById(id)) {
            throw new IllegalArgumentException("Interface configuration not found: " + id);
        }
        
        configRepository.deleteById(id);
    }

    @Override
    public Map<String, String> validateConfig(ExternalInterfaceConfig config) {
        Map<String, String> errors = new HashMap<>();
        
        // 基本字段验证
        if (!StringUtils.hasText(config.getSystemName())) {
            errors.put("systemName", "系统名称不能为空");
        }
        
        if (!StringUtils.hasText(config.getInterfaceName())) {
            errors.put("interfaceName", "接口名称不能为空");
        }
        
        if (!StringUtils.hasText(config.getInterfaceUrl())) {
            errors.put("interfaceUrl", "接口地址不能为空");
        } else {
            // 验证URL格式
            try {
                new java.net.URL(config.getInterfaceUrl());
            } catch (Exception e) {
                errors.put("interfaceUrl", "接口地址格式不正确");
            }
        }
        
        if (config.getProtocolType() == null) {
            errors.put("protocolType", "协议类型不能为空");
        }
        
        // 超时时间验证
        if (config.getTimeoutSeconds() != null && config.getTimeoutSeconds() <= 0) {
            errors.put("timeoutSeconds", "超时时间必须大于0");
        }
        
        // 重试次数验证
        if (config.getRetryTimes() != null && config.getRetryTimes() < 0) {
            errors.put("retryTimes", "重试次数不能小于0");
        }
        
        // 认证配置验证
        if (Boolean.TRUE.equals(config.getAuthRequired())) {
            if (config.getAuthInterfaceId() == null) {
                errors.put("authInterfaceId", "需要认证时必须指定认证接口ID");
            }
            if (!StringUtils.hasText(config.getTokenCacheKey())) {
                errors.put("tokenCacheKey", "需要认证时必须指定Token缓存键");
            }
        }
        
        return errors;
    }

    @Override
    public boolean testConnectivity(Long configId) {
        logger.info("Testing connectivity for interface configuration ID: {}", configId);
        
        Optional<ExternalInterfaceConfig> config = configRepository.findById(configId);
        if (config.isEmpty()) {
            throw new IllegalArgumentException("Interface configuration not found: " + configId);
        }
        
        try {
            // 这里应该根据协议类型进行实际的连通性测试
            // 简化实现，仅返回true
            // TODO: 实现真实的连通性测试
            return true;
        } catch (Exception e) {
            logger.error("Connectivity test failed for config ID: {}", configId, e);
            return false;
        }
    }
}