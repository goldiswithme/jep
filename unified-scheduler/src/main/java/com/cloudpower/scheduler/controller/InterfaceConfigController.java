package com.cloudpower.scheduler.controller;

import com.cloudpower.scheduler.entity.ExternalInterfaceConfig;
import com.cloudpower.scheduler.service.ExternalInterfaceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 外部接口配置管理控制器
 * External Interface Configuration Management Controller
 */
@RestController
@RequestMapping("/api/interface-config")
public class InterfaceConfigController {

    @Autowired
    private ExternalInterfaceConfigService configService;

    /**
     * 创建接口配置
     */
    @PostMapping
    public ResponseEntity<ExternalInterfaceConfig> createConfig(@Valid @RequestBody ExternalInterfaceConfig config) {
        ExternalInterfaceConfig saved = configService.save(config);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新接口配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExternalInterfaceConfig> updateConfig(
            @PathVariable Long id, @Valid @RequestBody ExternalInterfaceConfig config) {
        config.setId(id);
        ExternalInterfaceConfig updated = configService.update(config);
        return ResponseEntity.ok(updated);
    }

    /**
     * 获取接口配置详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExternalInterfaceConfig> getConfig(@PathVariable Long id) {
        return configService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取系统的所有接口配置
     */
    @GetMapping("/system/{systemName}")
    public ResponseEntity<List<ExternalInterfaceConfig>> getConfigsBySystem(@PathVariable String systemName) {
        List<ExternalInterfaceConfig> configs = configService.findEnabledBySystem(systemName);
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取所有启用的接口配置
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<ExternalInterfaceConfig>> getAllEnabledConfigs() {
        List<ExternalInterfaceConfig> configs = configService.findAllEnabled();
        return ResponseEntity.ok(configs);
    }

    /**
     * 启用/禁用接口配置
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleConfig(@PathVariable Long id, @RequestParam boolean enabled) {
        configService.toggleEnabled(id, enabled);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除接口配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        configService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 验证接口配置
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validateConfig(@RequestBody ExternalInterfaceConfig config) {
        Map<String, String> errors = configService.validateConfig(config);
        return ResponseEntity.ok(errors);
    }

    /**
     * 测试接口连通性
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnectivity(@PathVariable Long id) {
        boolean isConnected = configService.testConnectivity(id);
        Map<String, Object> result = Map.of(
                "configId", id,
                "connected", isConnected,
                "message", isConnected ? "连接成功" : "连接失败"
        );
        return ResponseEntity.ok(result);
    }
}