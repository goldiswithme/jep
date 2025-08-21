package com.cloudpower.scheduler.component.job;

import com.cloudpower.scheduler.service.InterfaceCallService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 外部接口调度任务处理器
 * External Interface Scheduling Job Handler
 */
@Component
public class InterfaceScheduleJobHandler {

    private static final Logger logger = LoggerFactory.getLogger(InterfaceScheduleJobHandler.class);

    @Autowired
    private InterfaceCallService callService;

    /**
     * 通用接口调用任务
     * 参数格式：systemName,interfaceName,requestData(JSON)
     */
    @XxlJob("genericInterfaceCallJob")
    public void genericInterfaceCallJob() throws Exception {
        XxlJobHelper.log("开始执行通用接口调用任务");
        
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().isEmpty()) {
            XxlJobHelper.log("任务参数为空，跳过执行");
            return;
        }
        
        try {
            // 解析参数：systemName,interfaceName,requestData
            String[] params = param.split(",", 3);
            if (params.length < 2) {
                throw new IllegalArgumentException("参数格式错误，应为：systemName,interfaceName[,requestData]");
            }
            
            String systemName = params[0].trim();
            String interfaceName = params[1].trim();
            Map<String, Object> requestData = new HashMap<>();
            
            if (params.length > 2 && !params[2].trim().isEmpty()) {
                // TODO: 解析JSON格式的请求数据
                // 这里简化处理，可以使用ObjectMapper解析JSON
                requestData.put("jobParam", params[2].trim());
            }
            
            String traceId = "job_" + UUID.randomUUID().toString();
            
            XxlJobHelper.log("开始调用接口：{}.{}, traceId: {}", systemName, interfaceName, traceId);
            
            // 执行接口调用
            Map<String, Object> result = callService.executeCall(systemName, interfaceName, requestData, traceId);
            
            XxlJobHelper.log("接口调用完成，结果：{}", result.toString());
            
        } catch (Exception e) {
            logger.error("通用接口调用任务执行失败", e);
            XxlJobHelper.log("任务执行失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 带认证的接口调用任务
     */
    @XxlJob("authInterfaceCallJob")
    public void authInterfaceCallJob() throws Exception {
        XxlJobHelper.log("开始执行带认证的接口调用任务");
        
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().isEmpty()) {
            XxlJobHelper.log("任务参数为空，跳过执行");
            return;
        }
        
        try {
            String[] params = param.split(",", 4);
            if (params.length < 2) {
                throw new IllegalArgumentException("参数格式错误，应为：systemName,interfaceName[,forceRefresh,requestData]");
            }
            
            String systemName = params[0].trim();
            String interfaceName = params[1].trim();
            boolean forceRefresh = params.length > 2 ? Boolean.parseBoolean(params[2].trim()) : false;
            
            Map<String, Object> requestData = new HashMap<>();
            if (params.length > 3 && !params[3].trim().isEmpty()) {
                requestData.put("jobParam", params[3].trim());
            }
            
            String traceId = "auth_job_" + UUID.randomUUID().toString();
            
            XxlJobHelper.log("开始调用带认证接口：{}.{}, forceRefresh: {}, traceId: {}", 
                           systemName, interfaceName, forceRefresh, traceId);
            
            // 执行带认证的接口调用
            Map<String, Object> result = callService.executeCallWithAuth(
                systemName, interfaceName, requestData, traceId, forceRefresh);
            
            XxlJobHelper.log("带认证接口调用完成，结果：{}", result.toString());
            
        } catch (Exception e) {
            logger.error("带认证接口调用任务执行失败", e);
            XxlJobHelper.log("任务执行失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 数据同步任务
     * 可配置定时同步外部系统数据
     */
    @XxlJob("dataSyncJob")
    public void dataSyncJob() throws Exception {
        XxlJobHelper.log("开始执行数据同步任务");
        
        try {
            // 这里可以配置多个系统的数据同步逻辑
            // 示例：同步系统A的数据
            syncSystemData("systemA");
            
            XxlJobHelper.log("数据同步任务执行完成");
            
        } catch (Exception e) {
            logger.error("数据同步任务执行失败", e);
            XxlJobHelper.log("数据同步任务执行失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * Token刷新任务
     * 定时刷新各系统的认证Token
     */
    @XxlJob("tokenRefreshJob")
    public void tokenRefreshJob() throws Exception {
        XxlJobHelper.log("开始执行Token刷新任务");
        
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().isEmpty()) {
            XxlJobHelper.log("任务参数为空，跳过执行");
            return;
        }
        
        try {
            // 参数可以是系统名称列表，用逗号分隔
            String[] systemNames = param.split(",");
            
            for (String systemName : systemNames) {
                systemName = systemName.trim();
                if (!systemName.isEmpty()) {
                    XxlJobHelper.log("刷新系统Token：{}", systemName);
                    
                    // 强制刷新Token
                    String token = callService.getAuthToken(systemName, true);
                    
                    if (token != null) {
                        XxlJobHelper.log("系统 {} Token刷新成功", systemName);
                    } else {
                        XxlJobHelper.log("系统 {} Token刷新失败", systemName);
                    }
                }
            }
            
            XxlJobHelper.log("Token刷新任务执行完成");
            
        } catch (Exception e) {
            logger.error("Token刷新任务执行失败", e);
            XxlJobHelper.log("Token刷新任务执行失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 私有方法：同步系统数据
     */
    private void syncSystemData(String systemName) {
        try {
            // 这里实现具体的数据同步逻辑
            // 1. 调用外部系统获取数据
            // 2. 处理和转换数据
            // 3. 保存到MQ或数据库
            
            Map<String, Object> requestData = new HashMap<>();
            String traceId = "sync_" + systemName + "_" + System.currentTimeMillis();
            
            // 示例：调用数据查询接口
            Map<String, Object> result = callService.executeCall(systemName, "dataQuery", requestData, traceId);
            
            XxlJobHelper.log("系统 {} 数据同步完成，数据量：{}", systemName, 
                           result.getOrDefault("dataCount", "未知"));
            
        } catch (Exception e) {
            logger.error("系统 {} 数据同步失败", systemName, e);
            XxlJobHelper.log("系统 {} 数据同步失败：{}", systemName, e.getMessage());
        }
    }
}