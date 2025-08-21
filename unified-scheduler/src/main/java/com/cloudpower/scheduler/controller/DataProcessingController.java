package com.cloudpower.scheduler.controller;

import com.cloudpower.scheduler.service.DataProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据处理控制器
 * Data Processing Controller
 */
@RestController
@RequestMapping("/api/data-processing")
public class DataProcessingController {

    @Autowired
    private DataProcessingService dataProcessingService;

    /**
     * 处理和转换数据
     */
    @PostMapping("/transform")
    public ResponseEntity<Map<String, Object>> processAndTransform(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rawData = (Map<String, Object>) request.get("rawData");
        String transformScript = (String) request.get("transformScript");
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");

        Map<String, Object> result = dataProcessingService.processAndTransform(
                rawData, transformScript, systemName, interfaceName);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 发送数据到MQ
     */
    @PostMapping("/send-to-mq")
    public ResponseEntity<Map<String, Object>> sendToMQ(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> processedData = (Map<String, Object>) request.get("processedData");
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");
        String traceId = (String) request.get("traceId");

        dataProcessingService.sendToMQ(processedData, systemName, interfaceName, traceId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "数据已发送到MQ",
                "traceId", traceId
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 通过API保存数据
     */
    @PostMapping("/save-via-api")
    public ResponseEntity<Map<String, Object>> saveViaAPI(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> processedData = (Map<String, Object>) request.get("processedData");
        String systemName = (String) request.get("systemName");
        String interfaceName = (String) request.get("interfaceName");
        String traceId = (String) request.get("traceId");

        Map<String, Object> result = dataProcessingService.saveViaAPI(
                processedData, systemName, interfaceName, traceId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 验证转换脚本
     */
    @PostMapping("/validate-script")
    public ResponseEntity<Map<String, Object>> validateScript(@RequestBody Map<String, Object> request) {
        String script = (String) request.get("script");
        
        Map<String, Object> result = dataProcessingService.validateScript(script);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试数据转换
     */
    @PostMapping("/test-transformation")
    public ResponseEntity<Map<String, Object>> testTransformation(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sampleData = (Map<String, Object>) request.get("sampleData");
        String transformScript = (String) request.get("transformScript");
        
        Map<String, Object> result = dataProcessingService.testTransformation(sampleData, transformScript);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取支持的转换函数
     */
    @GetMapping("/supported-functions")
    public ResponseEntity<List<Map<String, Object>>> getSupportedFunctions() {
        List<Map<String, Object>> functions = dataProcessingService.getSupportedFunctions();
        return ResponseEntity.ok(functions);
    }
}