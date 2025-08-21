package com.cloudpower.scheduler.service;

import java.util.Map;

/**
 * 接口调用服务接口
 * Interface Call Service Interface
 */
public interface InterfaceCallService {

    /**
     * 执行接口调用
     * Execute interface call
     * 
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @param requestData 请求数据
     * @param traceId 追踪ID
     * @return 响应结果
     */
    Map<String, Object> executeCall(String systemName, String interfaceName, 
                                   Map<String, Object> requestData, String traceId);

    /**
     * 执行接口调用（带认证）
     * Execute interface call with authentication
     * 
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @param requestData 请求数据
     * @param traceId 追踪ID
     * @param forceRefreshAuth 是否强制刷新认证
     * @return 响应结果
     */
    Map<String, Object> executeCallWithAuth(String systemName, String interfaceName, 
                                           Map<String, Object> requestData, String traceId, 
                                           boolean forceRefreshAuth);

    /**
     * 批量执行接口调用
     * Execute batch interface calls
     * 
     * @param callRequests 调用请求列表
     * @param traceId 追踪ID
     * @return 响应结果列表
     */
    Map<String, Object> executeBatchCalls(java.util.List<Map<String, Object>> callRequests, String traceId);

    /**
     * 异步执行接口调用
     * Execute interface call asynchronously
     * 
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @param requestData 请求数据
     * @param traceId 追踪ID
     * @param callback 回调处理器
     */
    void executeCallAsync(String systemName, String interfaceName, 
                         Map<String, Object> requestData, String traceId,
                         java.util.function.Consumer<Map<String, Object>> callback);

    /**
     * 获取认证Token
     * Get authentication token
     * 
     * @param systemName 系统名称
     * @param forceRefresh 是否强制刷新
     * @return Token值
     */
    String getAuthToken(String systemName, boolean forceRefresh);

    /**
     * 清除认证Token缓存
     * Clear authentication token cache
     * 
     * @param systemName 系统名称
     */
    void clearAuthTokenCache(String systemName);
}