package com.cloudpower.scheduler.service.impl;

import com.cloudpower.scheduler.service.DataProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

/**
 * 数据处理与转换服务实现
 * Data Processing and Transformation Service Implementation
 */
@Service
public class DataProcessingServiceImpl implements DataProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DataProcessingServiceImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${data-processing.transformation.script-engine:groovy}")
    private String scriptEngineName;

    @Value("${data-processing.persistence.api.base-url:http://localhost:8081/api}")
    private String saveApiBaseUrl;

    @Value("${data-processing.persistence.api.retry-times:3}")
    private int apiRetryTimes;

    @Value("${data-processing.persistence.api.retry-interval:1000}")
    private int apiRetryInterval;

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    @Override
    public Map<String, Object> processAndTransform(Map<String, Object> rawData, 
                                                  String transformScript,
                                                  String systemName, 
                                                  String interfaceName) {
        logger.info("开始处理和转换数据，系统：{}，接口：{}", systemName, interfaceName);
        
        try {
            Map<String, Object> processedData = new HashMap<>(rawData);
            
            // 添加处理元数据
            processedData.put("_metadata", Map.of(
                "systemName", systemName,
                "interfaceName", interfaceName,
                "processTime", new Date(),
                "version", "1.0"
            ));
            
            // 如果有转换脚本，执行脚本转换
            if (transformScript != null && !transformScript.trim().isEmpty()) {
                processedData = executeTransformScript(processedData, transformScript);
            }
            
            logger.info("数据处理和转换完成，原始数据大小：{}，处理后数据大小：{}", 
                       rawData.size(), processedData.size());
            
            return processedData;
            
        } catch (Exception e) {
            logger.error("数据处理和转换失败", e);
            throw new RuntimeException("数据处理和转换失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendToMQ(Map<String, Object> processedData, 
                        String systemName, 
                        String interfaceName, 
                        String traceId) {
        logger.info("发送处理后的数据到MQ，系统：{}，接口：{}，追踪ID：{}", systemName, interfaceName, traceId);
        
        try {
            // 构建MQ消息
            Map<String, Object> mqMessage = new HashMap<>();
            mqMessage.put("data", processedData);
            mqMessage.put("systemName", systemName);
            mqMessage.put("interfaceName", interfaceName);
            mqMessage.put("traceId", traceId);
            mqMessage.put("timestamp", new Date());
            
            // 发送到RabbitMQ
            rabbitTemplate.convertAndSend(mqMessage);
            
            logger.info("数据已发送到MQ，追踪ID：{}", traceId);
            
        } catch (Exception e) {
            logger.error("发送数据到MQ失败，追踪ID：{}", traceId, e);
            throw new RuntimeException("发送数据到MQ失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> saveViaAPI(Map<String, Object> processedData, 
                                         String systemName, 
                                         String interfaceName, 
                                         String traceId) {
        logger.info("通过统一保存API保存数据，系统：{}，接口：{}，追踪ID：{}", systemName, interfaceName, traceId);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= apiRetryTimes; attempt++) {
            try {
                // 构建保存请求
                Map<String, Object> saveRequest = new HashMap<>();
                saveRequest.put("data", processedData);
                saveRequest.put("source", Map.of(
                    "systemName", systemName,
                    "interfaceName", interfaceName,
                    "traceId", traceId
                ));
                saveRequest.put("timestamp", new Date());
                
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Trace-Id", traceId);
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(saveRequest, headers);
                
                // 调用保存API
                String saveUrl = saveApiBaseUrl + "/data/save";
                ResponseEntity<Map> response = restTemplate.exchange(
                    saveUrl, HttpMethod.POST, entity, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseBody = response.getBody();
                    
                    logger.info("数据保存成功，追踪ID：{}，响应：{}", traceId, responseBody);
                    
                    return responseBody != null ? responseBody : Map.of("success", true);
                } else {
                    throw new RuntimeException("保存API返回错误状态码: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("数据保存失败，尝试次数：{}/{}，追踪ID：{}，错误：{}", 
                           attempt, apiRetryTimes, traceId, e.getMessage());
                
                if (attempt < apiRetryTimes) {
                    try {
                        Thread.sleep(apiRetryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }
        }
        
        logger.error("数据保存失败，已达到最大重试次数，追踪ID：{}", traceId, lastException);
        throw new RuntimeException("数据保存失败，已重试" + apiRetryTimes + "次: " + 
                                 (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    @Override
    public Map<String, Object> validateScript(String script) {
        logger.info("验证转换脚本，脚本长度：{}", script != null ? script.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        if (script == null || script.trim().isEmpty()) {
            result.put("valid", true);
            result.put("message", "空脚本，验证通过");
            return result;
        }
        
        try {
            ScriptEngine engine = getScriptEngine();
            
            // 创建测试数据
            Map<String, Object> testData = Map.of(
                "test", "value",
                "number", 123,
                "list", Arrays.asList(1, 2, 3)
            );
            
            // 设置脚本变量
            engine.put("data", testData);
            engine.put("utils", new ScriptUtils());
            
            // 执行脚本
            Object scriptResult = engine.eval(script);
            
            result.put("valid", true);
            result.put("message", "脚本验证成功");
            result.put("testResult", scriptResult);
            
            logger.info("脚本验证成功");
            
        } catch (Exception e) {
            logger.error("脚本验证失败", e);
            result.put("valid", false);
            result.put("message", "脚本验证失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testTransformation(Map<String, Object> sampleData, String transformScript) {
        logger.info("测试数据转换，样本数据大小：{}，脚本长度：{}", 
                   sampleData.size(), transformScript != null ? transformScript.length() : 0);
        
        try {
            Map<String, Object> result = executeTransformScript(sampleData, transformScript);
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("success", true);
            testResult.put("originalData", sampleData);
            testResult.put("transformedData", result);
            testResult.put("message", "转换测试成功");
            
            return testResult;
            
        } catch (Exception e) {
            logger.error("转换测试失败", e);
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("success", false);
            testResult.put("originalData", sampleData);
            testResult.put("error", e.getMessage());
            testResult.put("message", "转换测试失败: " + e.getMessage());
            
            return testResult;
        }
    }

    @Override
    public List<Map<String, Object>> getSupportedFunctions() {
        List<Map<String, Object>> functions = new ArrayList<>();
        
        // 内置工具函数
        functions.add(createFunctionInfo("formatDate", "格式化日期", 
                                       "utils.formatDate(date, 'yyyy-MM-dd HH:mm:ss')"));
        functions.add(createFunctionInfo("parseJson", "解析JSON字符串", 
                                       "utils.parseJson(jsonString)"));
        functions.add(createFunctionInfo("toJson", "转换为JSON字符串", 
                                       "utils.toJson(object)"));
        functions.add(createFunctionInfo("isEmpty", "检查是否为空", 
                                       "utils.isEmpty(value)"));
        functions.add(createFunctionInfo("defaultValue", "设置默认值", 
                                       "utils.defaultValue(value, defaultVal)"));
        functions.add(createFunctionInfo("extractField", "提取字段值", 
                                       "utils.extractField(data, 'field.nested.path')"));
        functions.add(createFunctionInfo("transformArray", "转换数组", 
                                       "utils.transformArray(array, transformFunction)"));
        
        // 数据验证函数
        functions.add(createFunctionInfo("validateEmail", "验证邮箱格式", 
                                       "utils.validateEmail(email)"));
        functions.add(createFunctionInfo("validatePhone", "验证手机号格式", 
                                       "utils.validatePhone(phone)"));
        
        return functions;
    }

    // 私有辅助方法
    
    private Map<String, Object> executeTransformScript(Map<String, Object> data, String script) throws ScriptException {
        ScriptEngine engine = getScriptEngine();
        
        // 设置脚本变量
        engine.put("data", new HashMap<>(data));
        engine.put("utils", new ScriptUtils());
        
        // 执行脚本
        Object result = engine.eval(script);
        
        // 处理脚本返回结果
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapResult = (Map<String, Object>) result;
            return mapResult;
        } else {
            // 如果脚本没有返回Map，返回原始数据
            return data;
        }
    }

    private ScriptEngine getScriptEngine() {
        ScriptEngine engine = scriptEngineManager.getEngineByName(scriptEngineName);
        if (engine == null) {
            // 如果指定的脚本引擎不可用，使用JavaScript引擎
            engine = scriptEngineManager.getEngineByName("javascript");
            if (engine == null) {
                throw new RuntimeException("没有可用的脚本引擎");
            }
        }
        return engine;
    }

    private Map<String, Object> createFunctionInfo(String name, String description, String example) {
        Map<String, Object> functionInfo = new HashMap<>();
        functionInfo.put("name", name);
        functionInfo.put("description", description);
        functionInfo.put("example", example);
        return functionInfo;
    }

    /**
     * 脚本工具类
     * 提供在转换脚本中使用的工具函数
     */
    public static class ScriptUtils {
        
        public String formatDate(Date date, String pattern) {
            if (date == null) return null;
            return new java.text.SimpleDateFormat(pattern).format(date);
        }
        
        public Map<String, Object> parseJson(String jsonString) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> result = mapper.readValue(jsonString, Map.class);
                return result;
            } catch (Exception e) {
                throw new RuntimeException("JSON解析失败: " + e.getMessage());
            }
        }
        
        public String toJson(Object object) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(object);
            } catch (Exception e) {
                throw new RuntimeException("JSON转换失败: " + e.getMessage());
            }
        }
        
        public boolean isEmpty(Object value) {
            if (value == null) return true;
            if (value instanceof String) return ((String) value).trim().isEmpty();
            if (value instanceof Collection) return ((Collection<?>) value).isEmpty();
            if (value instanceof Map) return ((Map<?, ?>) value).isEmpty();
            return false;
        }
        
        public Object defaultValue(Object value, Object defaultVal) {
            return isEmpty(value) ? defaultVal : value;
        }
        
        public Object extractField(Map<String, Object> data, String fieldPath) {
            if (data == null || fieldPath == null) return null;
            
            String[] paths = fieldPath.split("\\.");
            Object current = data;
            
            for (String path : paths) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    current = map.get(path);
                } else {
                    return null;
                }
            }
            
            return current;
        }
        
        public List<Object> transformArray(List<Object> array, String transformExpression) {
            // 这里可以实现数组转换逻辑
            // 简化实现，直接返回原数组
            return array;
        }
        
        public boolean validateEmail(String email) {
            if (email == null) return false;
            return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
        }
        
        public boolean validatePhone(String phone) {
            if (phone == null) return false;
            return phone.matches("^1[3-9]\\d{9}$");
        }
    }
}