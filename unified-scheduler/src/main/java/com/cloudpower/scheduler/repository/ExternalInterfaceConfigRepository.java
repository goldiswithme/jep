package com.cloudpower.scheduler.repository;

import com.cloudpower.scheduler.entity.ExternalInterfaceConfig;
import com.cloudpower.scheduler.enums.ProtocolType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 外部接口配置数据访问层
 * External Interface Configuration Repository
 */
@Repository
public interface ExternalInterfaceConfigRepository extends JpaRepository<ExternalInterfaceConfig, Long> {

    /**
     * 根据系统名称查找接口配置
     * Find interface configurations by system name
     */
    List<ExternalInterfaceConfig> findBySystemNameAndEnabledTrue(String systemName);

    /**
     * 根据协议类型查找接口配置
     * Find interface configurations by protocol type
     */
    List<ExternalInterfaceConfig> findByProtocolTypeAndEnabledTrue(ProtocolType protocolType);

    /**
     * 根据系统名称和接口名称查找配置
     * Find configuration by system name and interface name
     */
    Optional<ExternalInterfaceConfig> findBySystemNameAndInterfaceNameAndEnabledTrue(
            String systemName, String interfaceName);

    /**
     * 查找需要认证的接口配置
     * Find interface configurations that require authentication
     */
    @Query("SELECT e FROM ExternalInterfaceConfig e WHERE e.authRequired = true AND e.enabled = true")
    List<ExternalInterfaceConfig> findAuthRequiredInterfaces();

    /**
     * 查找认证接口配置
     * Find authentication interface configurations
     */
    @Query("SELECT e FROM ExternalInterfaceConfig e WHERE e.id IN " +
           "(SELECT DISTINCT ic.authInterfaceId FROM ExternalInterfaceConfig ic WHERE ic.authInterfaceId IS NOT NULL) " +
           "AND e.enabled = true")
    List<ExternalInterfaceConfig> findAuthInterfaces();

    /**
     * 根据系统名称查找启用的接口数量
     * Count enabled interfaces by system name
     */
    long countBySystemNameAndEnabledTrue(String systemName);
}