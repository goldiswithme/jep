package com.cloudpower.scheduler.service.impl;

import com.cloudpower.scheduler.entity.ExternalInterfaceConfig;
import com.cloudpower.scheduler.entity.InterfaceCallLog;
import com.cloudpower.scheduler.enums.CallStatus;
import com.cloudpower.scheduler.repository.InterfaceCallLogRepository;
import com.cloudpower.scheduler.service.ExternalInterfaceConfigService;
import com.cloudpower.scheduler.service.InterfaceCallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 接口调用服务实现
 * Interface Call Service Implementation
 */
@Service
public class InterfaceCallServiceImpl implements InterfaceCallService {

    private static final Logger logger = LoggerFactory.getLogger(InterfaceCallServiceImpl.class);

    @Autowired
    private ExternalInterfaceConfigService configService;

    @Autowired
    private InterfaceCallLogRepository callLogRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> executeCall(String systemName, String interfaceName, 
                                         Map<String, Object> requestData, String traceId) {
        return executeCallWithAuth(systemName, interfaceName, requestData, traceId, false);
    }

    @Override
    public Map<String, Object> executeCallWithAuth(String systemName, String interfaceName, 
                                                  Map<String, Object> requestData, String traceId, 
                                                  boolean forceRefreshAuth) {
        logger.info("Executing interface call: {}.{} with traceId: {}", systemName, interfaceName, traceId);
        
        // 获取接口配置
        Optional<ExternalInterfaceConfig> configOpt = configService.findBySystemAndInterface(systemName, interfaceName);
        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Interface configuration not found: %s.%s", systemName, interfaceName));
        }
        
        ExternalInterfaceConfig config = configOpt.get();
        InterfaceCallLog callLog = createCallLog(config, traceId);
        
        try {
            // 处理认证
            if (Boolean.TRUE.equals(config.getAuthRequired())) {
                String token = getAuthToken(systemName, forceRefreshAuth);
                if (StringUtils.hasText(token)) {
                    // 将token添加到请求数据或headers中
                    addAuthTokenToRequest(requestData, config, token);
                }
            }
            
            // 构建请求
            Map<String, Object> processedRequest = buildRequest(config, requestData);
            callLog.setRequestBody(toJsonString(processedRequest));
            
            // 执行调用
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = performCall(config, processedRequest, callLog);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录成功日志
            callLog.setExecutionTimeMs(executionTime);
            callLog.setCallStatus(CallStatus.SUCCESS);
            callLog.setResponseBody(toJsonString(response));
            
            logger.info("Interface call completed successfully: {}.{}, execution time: {}ms", 
                       systemName, interfaceName, executionTime);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Interface call failed: {}.{}", systemName, interfaceName, e);
            
            callLog.setCallStatus(CallStatus.FAILED);
            callLog.setErrorMessage(e.getMessage());
            
            // 重试逻辑
            if (config.getRetryTimes() != null && config.getRetryTimes() > 0) {
                return retryCall(config, requestData, traceId, callLog, e);
            }
            
            throw new RuntimeException("Interface call failed: " + e.getMessage(), e);
        } finally {
            // 保存调用日志
            callLogRepository.save(callLog);
        }
    }

    @Override
    public Map<String, Object> executeBatchCalls(List<Map<String, Object>> callRequests, String traceId) {
        logger.info("Executing batch interface calls, count: {}, traceId: {}", callRequests.size(), traceId);
        
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> successResults = new ArrayList<>();
        List<Map<String, Object>> failedResults = new ArrayList<>();
        
        for (int i = 0; i < callRequests.size(); i++) {
            Map<String, Object> request = callRequests.get(i);
            String systemName = (String) request.get("systemName");
            String interfaceName = (String) request.get("interfaceName");
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) request.get("requestData");
            
            String batchTraceId = traceId + "_batch_" + i;
            
            try {
                Map<String, Object> result = executeCall(systemName, interfaceName, requestData, batchTraceId);
                Map<String, Object> successResult = new HashMap<>();
                successResult.put("index", i);
                successResult.put("systemName", systemName);
                successResult.put("interfaceName", interfaceName);
                successResult.put("result", result);
                successResults.add(successResult);
            } catch (Exception e) {
                Map<String, Object> failedResult = new HashMap<>();
                failedResult.put("index", i);
                failedResult.put("systemName", systemName);
                failedResult.put("interfaceName", interfaceName);
                failedResult.put("error", e.getMessage());
                failedResults.add(failedResult);
            }
        }
        
        results.put("success", successResults);
        results.put("failed", failedResults);
        results.put("totalCount", callRequests.size());
        results.put("successCount", successResults.size());
        results.put("failedCount", failedResults.size());
        
        return results;
    }

    @Override
    public void executeCallAsync(String systemName, String interfaceName, 
                                Map<String, Object> requestData, String traceId,
                                Consumer<Map<String, Object>> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return executeCall(systemName, interfaceName, requestData, traceId);
            } catch (Exception e) {
                logger.error("Async interface call failed: {}.{}", systemName, interfaceName, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                errorResult.put("success", false);
                return errorResult;
            }
        }).thenAccept(callback);
    }

    @Override
    public String getAuthToken(String systemName, boolean forceRefresh) {
        String cacheKey = "auth_token:" + systemName;
        
        // 如果不强制刷新，先尝试从缓存获取
        if (!forceRefresh) {
            Object cachedToken = redisTemplate.opsForValue().get(cacheKey);
            if (cachedToken != null) {
                logger.debug("Retrieved auth token from cache for system: {}", systemName);
                return cachedToken.toString();
            }
        }
        
        // 查找认证接口配置
        List<ExternalInterfaceConfig> authConfigs = configService.findEnabledBySystem(systemName);
        Optional<ExternalInterfaceConfig> authConfig = authConfigs.stream()
                .filter(config -> config.getInterfaceName().toLowerCase().contains("login") || 
                                config.getInterfaceName().toLowerCase().contains("auth"))
                .findFirst();
        
        if (authConfig.isEmpty()) {
            logger.warn("No authentication interface found for system: {}", systemName);
            return null;
        }
        
        try {
            // 执行认证接口调用
            ExternalInterfaceConfig config = authConfig.get();
            Map<String, Object> authRequest = buildAuthRequest(config);
            Map<String, Object> authResponse = performCall(config, authRequest, null);
            
            // 从响应中提取token
            String token = extractTokenFromResponse(authResponse, config);
            
            if (StringUtils.hasText(token)) {
                // 缓存token
                Duration expiration = Duration.ofSeconds(
                    config.getTokenExpireSeconds() != null ? config.getTokenExpireSeconds() : 7200);
                redisTemplate.opsForValue().set(cacheKey, token, expiration);
                
                logger.info("Auth token cached for system: {}, expiration: {}s", systemName, expiration.getSeconds());
                return token;
            }
            
        } catch (Exception e) {
            logger.error("Failed to get auth token for system: {}", systemName, e);
        }
        
        return null;
    }

    @Override
    public void clearAuthTokenCache(String systemName) {
        String cacheKey = "auth_token:" + systemName;
        redisTemplate.delete(cacheKey);
        logger.info("Auth token cache cleared for system: {}", systemName);
    }

    // 私有辅助方法
    
    private InterfaceCallLog createCallLog(ExternalInterfaceConfig config, String traceId) {
        InterfaceCallLog callLog = new InterfaceCallLog();
        callLog.setInterfaceConfigId(config.getId());
        callLog.setSystemName(config.getSystemName());
        callLog.setInterfaceName(config.getInterfaceName());
        callLog.setRequestUrl(config.getInterfaceUrl());
        callLog.setHttpMethod(config.getHttpMethod());
        callLog.setTraceId(traceId);
        callLog.setCallStatus(CallStatus.PENDING);
        return callLog;
    }

    private void addAuthTokenToRequest(Map<String, Object> requestData, ExternalInterfaceConfig config, String token) {
        // 这里可以根据具体需求将token添加到请求的不同位置
        // 例如：headers、body、query参数等
        requestData.put("token", token);
        requestData.put("authorization", "Bearer " + token);
    }

    private Map<String, Object> buildRequest(ExternalInterfaceConfig config, Map<String, Object> requestData) {
        Map<String, Object> request = new HashMap<>();
        
        // 如果有请求模板，使用模板
        if (StringUtils.hasText(config.getRequestTemplate())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> template = objectMapper.readValue(config.getRequestTemplate(), Map.class);
                request.putAll(template);
            } catch (Exception e) {
                logger.warn("Failed to parse request template for {}.{}: {}", 
                           config.getSystemName(), config.getInterfaceName(), e.getMessage());
            }
        }
        
        // 合并请求数据
        if (requestData != null) {
            request.putAll(requestData);
        }
        
        return request;
    }

    private Map<String, Object> performCall(ExternalInterfaceConfig config, Map<String, Object> requestData, 
                                           InterfaceCallLog callLog) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 添加自定义headers
        if (StringUtils.hasText(config.getHeaders())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> customHeaders = objectMapper.readValue(config.getHeaders(), Map.class);
                customHeaders.forEach(headers::set);
            } catch (Exception e) {
                logger.warn("Failed to parse custom headers: {}", e.getMessage());
            }
        }
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            config.getInterfaceUrl(),
            HttpMethod.valueOf(config.getHttpMethod().toUpperCase()),
            entity,
            Map.class
        );
        
        if (callLog != null) {
            callLog.setResponseStatus(response.getStatusCodeValue());
            callLog.setResponseHeaders(toJsonString(response.getHeaders().toSingleValueMap()));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = response.getBody();
        return responseBody != null ? responseBody : new HashMap<>();
    }

    private Map<String, Object> retryCall(ExternalInterfaceConfig config, Map<String, Object> requestData, 
                                         String traceId, InterfaceCallLog originalLog, Exception originalException) {
        int retryTimes = config.getRetryTimes();
        int retryInterval = config.getRetryIntervalSeconds() != null ? config.getRetryIntervalSeconds() : 5;
        
        for (int i = 1; i <= retryTimes; i++) {
            try {
                logger.info("Retrying interface call: {}.{}, attempt: {}/{}", 
                           config.getSystemName(), config.getInterfaceName(), i, retryTimes);
                
                // 等待重试间隔
                Thread.sleep(retryInterval * 1000L);
                
                // 创建重试日志
                InterfaceCallLog retryLog = createCallLog(config, traceId + "_retry_" + i);
                retryLog.setRetryCount(i);
                
                Map<String, Object> processedRequest = buildRequest(config, requestData);
                retryLog.setRequestBody(toJsonString(processedRequest));
                
                long startTime = System.currentTimeMillis();
                Map<String, Object> response = performCall(config, processedRequest, retryLog);
                long executionTime = System.currentTimeMillis() - startTime;
                
                retryLog.setExecutionTimeMs(executionTime);
                retryLog.setCallStatus(CallStatus.SUCCESS);
                retryLog.setResponseBody(toJsonString(response));
                callLogRepository.save(retryLog);
                
                logger.info("Interface call retry succeeded: {}.{}, attempt: {}", 
                           config.getSystemName(), config.getInterfaceName(), i);
                
                return response;
                
            } catch (Exception e) {
                logger.warn("Interface call retry failed: {}.{}, attempt: {}, error: {}", 
                           config.getSystemName(), config.getInterfaceName(), i, e.getMessage());
                
                if (i == retryTimes) {
                    // 最后一次重试也失败了
                    throw new RuntimeException("All retry attempts failed. Original error: " + 
                                             originalException.getMessage(), originalException);
                }
            }
        }
        
        throw new RuntimeException("Unexpected error in retry logic");
    }

    private Map<String, Object> buildAuthRequest(ExternalInterfaceConfig authConfig) {
        Map<String, Object> authRequest = new HashMap<>();
        
        if (StringUtils.hasText(authConfig.getRequestTemplate())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> template = objectMapper.readValue(authConfig.getRequestTemplate(), Map.class);
                authRequest.putAll(template);
            } catch (Exception e) {
                logger.warn("Failed to parse auth request template: {}", e.getMessage());
            }
        }
        
        return authRequest;
    }

    private String extractTokenFromResponse(Map<String, Object> response, ExternalInterfaceConfig config) {
        // 这里应该根据具体的响应格式来提取token
        // 常见的格式包括：token、access_token、accessToken等字段
        
        if (response.containsKey("token")) {
            return response.get("token").toString();
        }
        
        if (response.containsKey("access_token")) {
            return response.get("access_token").toString();
        }
        
        if (response.containsKey("accessToken")) {
            return response.get("accessToken").toString();
        }
        
        // 如果有嵌套的数据结构
        if (response.containsKey("data")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data != null) {
                if (data.containsKey("token")) {
                    return data.get("token").toString();
                }
                if (data.containsKey("access_token")) {
                    return data.get("access_token").toString();
                }
            }
        }
        
        logger.warn("Could not extract token from auth response for system: {}", config.getSystemName());
        return null;
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.warn("Failed to convert object to JSON string: {}", e.getMessage());
            return obj != null ? obj.toString() : null;
        }
    }
}