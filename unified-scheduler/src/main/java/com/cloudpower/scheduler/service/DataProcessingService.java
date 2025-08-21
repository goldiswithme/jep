package com.cloudpower.scheduler.service;

import java.util.Map;

/**
 * 数据处理与转换服务接口
 * Data Processing and Transformation Service Interface
 */
public interface DataProcessingService {

    /**
     * 处理和转换数据
     * Process and transform data
     * 
     * @param rawData 原始数据
     * @param transformScript 转换脚本
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @return 处理后的数据
     */
    Map<String, Object> processAndTransform(Map<String, Object> rawData, 
                                           String transformScript,
                                           String systemName, 
                                           String interfaceName);

    /**
     * 发送处理后的数据到MQ
     * Send processed data to MQ
     * 
     * @param processedData 处理后的数据
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @param traceId 追踪ID
     */
    void sendToMQ(Map<String, Object> processedData, 
                  String systemName, 
                  String interfaceName, 
                  String traceId);

    /**
     * 通过统一保存API保存数据
     * Save data via unified save API
     * 
     * @param processedData 处理后的数据
     * @param systemName 系统名称
     * @param interfaceName 接口名称
     * @param traceId 追踪ID
     * @return 保存结果
     */
    Map<String, Object> saveViaAPI(Map<String, Object> processedData, 
                                  String systemName, 
                                  String interfaceName, 
                                  String traceId);

    /**
     * 验证转换脚本
     * Validate transformation script
     * 
     * @param script 转换脚本
     * @return 验证结果
     */
    Map<String, Object> validateScript(String script);

    /**
     * 测试数据转换
     * Test data transformation
     * 
     * @param sampleData 样本数据
     * @param transformScript 转换脚本
     * @return 转换结果
     */
    Map<String, Object> testTransformation(Map<String, Object> sampleData, String transformScript);

    /**
     * 获取支持的转换函数列表
     * Get supported transformation functions
     * 
     * @return 函数列表
     */
    java.util.List<Map<String, Object>> getSupportedFunctions();
}