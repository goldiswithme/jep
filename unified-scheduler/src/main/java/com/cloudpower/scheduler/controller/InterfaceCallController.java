package com.cloudpower.scheduler.controller;

import com.cloudpower.scheduler.service.InterfaceCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 接口调用控制器
 * Interface Call Controller
 */
@RestController
@RequestMapping("/api/interface-call")
public class InterfaceCallController {

    @Autowired
    private InterfaceCallService callService;

    /**
     * 执行单个接口调用
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeCall(@RequestBody Map<String, Object> request) {
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) request.get("requestData");
        String traceId = (String) request.getOrDefault("traceId", UUID.randomUUID().toString());

        Map<String, Object> result = callService.executeCall(systemName, interfaceName, requestData, traceId);
        return ResponseEntity.ok(result);
    }

    /**
     * 执行带认证的接口调用
     */
    @PostMapping("/execute-with-auth")
    public ResponseEntity<Map<String, Object>> executeCallWithAuth(@RequestBody Map<String, Object> request) {
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) request.get("requestData");
        String traceId = (String) request.getOrDefault("traceId", UUID.randomUUID().toString());
        Boolean forceRefreshAuth = (Boolean) request.getOrDefault("forceRefreshAuth", false);

        Map<String, Object> result = callService.executeCallWithAuth(
                systemName, interfaceName, requestData, traceId, forceRefreshAuth);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量执行接口调用
     */
    @PostMapping("/execute-batch")
    public ResponseEntity<Map<String, Object>> executeBatchCalls(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> callRequests = (List<Map<String, Object>>) request.get("callRequests");
        String traceId = (String) request.getOrDefault("traceId", UUID.randomUUID().toString());

        Map<String, Object> result = callService.executeBatchCalls(callRequests, traceId);
        return ResponseEntity.ok(result);
    }

    /**
     * 异步执行接口调用
     */
    @PostMapping("/execute-async")
    public ResponseEntity<Map<String, Object>> executeCallAsync(@RequestBody Map<String, Object> request) {
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) request.get("requestData");
        String traceId = (String) request.getOrDefault("traceId", UUID.randomUUID().toString());

        callService.executeCallAsync(systemName, interfaceName, requestData, traceId, result -> {
            // 这里可以添加异步回调处理逻辑
            // 例如：发送结果到MQ、记录到数据库等
        });

        Map<String, Object> response = Map.of(
                "message", "异步调用已提交",
                "traceId", traceId,
                "status", "SUBMITTED"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 获取认证Token
     */
    @PostMapping("/auth-token/{systemName}")
    public ResponseEntity<Map<String, Object>> getAuthToken(
            @PathVariable String systemName,
            @RequestParam(defaultValue = "false") boolean forceRefresh) {
        
        String token = callService.getAuthToken(systemName, forceRefresh);
        
        Map<String, Object> result = Map.of(
                "systemName", systemName,
                "token", token != null ? token : "",
                "hasToken", token != null
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除认证Token缓存
     */
    @DeleteMapping("/auth-token/{systemName}")
    public ResponseEntity<Map<String, Object>> clearAuthTokenCache(@PathVariable String systemName) {
        callService.clearAuthTokenCache(systemName);
        
        Map<String, Object> result = Map.of(
                "systemName", systemName,
                "message", "认证Token缓存已清除"
        );
        
        return ResponseEntity.ok(result);
    }
}