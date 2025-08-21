package com.cloudpower.scheduler.service;

import com.cloudpower.scheduler.entity.ExternalInterfaceConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 外部接口配置服务接口
 * External Interface Configuration Service Interface
 */
public interface ExternalInterfaceConfigService {

    /**
     * 保存接口配置
     * Save interface configuration
     */
    ExternalInterfaceConfig save(ExternalInterfaceConfig config);

    /**
     * 根据ID查找接口配置
     * Find interface configuration by ID
     */
    Optional<ExternalInterfaceConfig> findById(Long id);

    /**
     * 根据系统名称和接口名称查找配置
     * Find configuration by system name and interface name
     */
    Optional<ExternalInterfaceConfig> findBySystemAndInterface(String systemName, String interfaceName);

    /**
     * 查找系统的所有启用接口
     * Find all enabled interfaces for a system
     */
    List<ExternalInterfaceConfig> findEnabledBySystem(String systemName);

    /**
     * 查找所有启用的接口配置
     * Find all enabled interface configurations
     */
    List<ExternalInterfaceConfig> findAllEnabled();

    /**
     * 更新接口配置
     * Update interface configuration
     */
    ExternalInterfaceConfig update(ExternalInterfaceConfig config);

    /**
     * 启用/禁用接口
     * Enable/disable interface
     */
    void toggleEnabled(Long id, boolean enabled);

    /**
     * 删除接口配置
     * Delete interface configuration
     */
    void deleteById(Long id);

    /**
     * 验证接口配置
     * Validate interface configuration
     */
    Map<String, String> validateConfig(ExternalInterfaceConfig config);

    /**
     * 测试接口连通性
     * Test interface connectivity
     */
    boolean testConnectivity(Long configId);
}