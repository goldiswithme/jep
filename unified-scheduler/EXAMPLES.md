# 使用示例

## 快速开始

### 1. 配置外部系统接口

#### 1.1 创建认证接口配置
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/interface-config \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "powerSystem",
    "interfaceName": "login",
    "protocolType": "HTTP",
    "interfaceUrl": "http://power-system.com/api/auth/login",
    "httpMethod": "POST",
    "timeoutSeconds": 30,
    "retryTimes": 2,
    "authRequired": false,
    "requestTemplate": "{\"username\":\"api_user\",\"password\":\"api_pass\"}",
    "description": "功率系统登录接口"
  }'
```

#### 1.2 创建业务接口配置
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/interface-config \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "powerSystem",
    "interfaceName": "dataQuery",
    "protocolType": "HTTP",
    "interfaceUrl": "http://power-system.com/api/data/query",
    "httpMethod": "POST",
    "timeoutSeconds": 60,
    "retryTimes": 3,
    "authRequired": true,
    "authInterfaceId": 1,
    "tokenCacheKey": "power_system_token",
    "tokenExpireSeconds": 7200,
    "dataTransformScript": "// 数据转换脚本\ndata.queryTime = new Date();\ndata.source = \"powerSystem\";\nif (data.result && data.result.data) {\n  data.transformedData = data.result.data.map(item => ({\n    id: item.deviceId,\n    power: parseFloat(item.powerValue),\n    timestamp: new Date(item.collectTime),\n    status: item.deviceStatus === \"1\" ? \"ONLINE\" : \"OFFLINE\"\n  }));\n}\nreturn data;",
    "description": "功率数据查询接口"
  }'
```

### 2. 执行接口调用

#### 2.1 单个接口调用
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/interface-call/execute \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "powerSystem",
    "interfaceName": "dataQuery",
    "requestData": {
      "startTime": "2024-01-01 00:00:00",
      "endTime": "2024-01-01 23:59:59",
      "deviceIds": ["DEVICE001", "DEVICE002"]
    },
    "traceId": "manual_call_001"
  }'
```

#### 2.2 批量接口调用
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/interface-call/execute-batch \
  -H "Content-Type: application/json" \
  -d '{
    "callRequests": [
      {
        "systemName": "powerSystem",
        "interfaceName": "dataQuery",
        "requestData": {
          "startTime": "2024-01-01 00:00:00",
          "endTime": "2024-01-01 11:59:59",
          "deviceIds": ["DEVICE001"]
        }
      },
      {
        "systemName": "powerSystem", 
        "interfaceName": "dataQuery",
        "requestData": {
          "startTime": "2024-01-01 12:00:00",
          "endTime": "2024-01-01 23:59:59",
          "deviceIds": ["DEVICE002"]
        }
      }
    ],
    "traceId": "batch_call_001"
  }'
```

### 3. 数据处理与转换

#### 3.1 测试数据转换脚本
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/data-processing/test-transformation \
  -H "Content-Type: application/json" \
  -d '{
    "sampleData": {
      "result": {
        "data": [
          {
            "deviceId": "DEVICE001",
            "powerValue": "1250.5",
            "collectTime": "2024-01-01T10:30:00Z",
            "deviceStatus": "1"
          },
          {
            "deviceId": "DEVICE002",
            "powerValue": "890.2",
            "collectTime": "2024-01-01T10:30:00Z",
            "deviceStatus": "0"
          }
        ]
      }
    },
    "transformScript": "// 数据转换脚本\ndata.queryTime = new Date();\ndata.source = \"powerSystem\";\nif (data.result && data.result.data) {\n  data.transformedData = data.result.data.map(item => ({\n    id: item.deviceId,\n    power: parseFloat(item.powerValue),\n    timestamp: new Date(item.collectTime),\n    status: item.deviceStatus === \"1\" ? \"ONLINE\" : \"OFFLINE\"\n  }));\n}\nreturn data;"
  }'
```

#### 3.2 处理和转换数据
```bash
curl -X POST http://localhost:8080/unified-scheduler/api/data-processing/transform \
  -H "Content-Type: application/json" \
  -d '{
    "rawData": {
      "devices": [
        {"id": "D001", "value": 1200.5, "time": "2024-01-01T10:00:00Z"},
        {"id": "D002", "value": 850.3, "time": "2024-01-01T10:00:00Z"}
      ]
    },
    "transformScript": "// 转换为标准格式\ndata.processedDevices = data.devices.map(device => ({\n  deviceId: device.id,\n  powerValue: device.value,\n  timestamp: new Date(device.time),\n  unit: \"kW\"\n}));\ndata.totalPower = data.devices.reduce((sum, device) => sum + device.value, 0);\nreturn data;",
    "systemName": "powerSystem",
    "interfaceName": "dataQuery"
  }'
```

## XXL-Job任务配置示例

### 1. 定时数据同步任务

**任务配置:**
- 执行器: unified-scheduler
- JobHandler: genericInterfaceCallJob
- 参数: `powerSystem,dataQuery,{"syncType":"scheduled","batchSize":100}`
- Cron表达式: `0 */15 * * * ?` (每15分钟执行一次)

### 2. Token刷新任务

**任务配置:**
- 执行器: unified-scheduler
- JobHandler: tokenRefreshJob
- 参数: `powerSystem,weatherSystem,iotSystem`
- Cron表达式: `0 0 */2 * * ?` (每2小时执行一次)

### 3. 数据同步任务

**任务配置:**
- 执行器: unified-scheduler
- JobHandler: dataSyncJob
- 参数: 无
- Cron表达式: `0 0 1 * * ?` (每天凌晨1点执行)

## 完整业务流程示例

### 场景：功率预测系统定时从多个外部系统获取数据

#### 1. 系统配置

```bash
# 配置天气系统接口
curl -X POST http://localhost:8080/unified-scheduler/api/interface-config \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "weatherSystem",
    "interfaceName": "getWeatherData",
    "protocolType": "HTTP",
    "interfaceUrl": "http://weather-api.com/v1/current",
    "httpMethod": "GET",
    "timeoutSeconds": 30,
    "authRequired": true,
    "authInterfaceId": 2,
    "tokenCacheKey": "weather_api_token",
    "dataTransformScript": "// 天气数据转换\ndata.weather = {\n  temperature: parseFloat(data.current.temp_c),\n  humidity: parseFloat(data.current.humidity),\n  windSpeed: parseFloat(data.current.wind_kph),\n  condition: data.current.condition.text,\n  timestamp: new Date()\n};\nreturn data;"
  }'

# 配置IoT系统接口
curl -X POST http://localhost:8080/unified-scheduler/api/interface-config \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "iotSystem",
    "interfaceName": "getSensorData",
    "protocolType": "HTTP",
    "interfaceUrl": "http://iot-platform.com/api/sensors/data",
    "httpMethod": "POST",
    "timeoutSeconds": 45,
    "authRequired": true,
    "authInterfaceId": 3,
    "tokenCacheKey": "iot_system_token",
    "dataTransformScript": "// IoT数据转换\ndata.sensors = data.sensorData.map(sensor => ({\n  sensorId: sensor.id,\n  value: parseFloat(sensor.value),\n  unit: sensor.unit,\n  location: sensor.location,\n  timestamp: new Date(sensor.timestamp)\n}));\nreturn data;"
  }'
```

#### 2. 编排数据收集流程

```bash
# 创建数据收集任务
curl -X POST http://localhost:8080/unified-scheduler/api/interface-call/execute-batch \
  -H "Content-Type: application/json" \
  -d '{
    "callRequests": [
      {
        "systemName": "powerSystem",
        "interfaceName": "dataQuery",
        "requestData": {
          "startTime": "2024-01-01T10:00:00Z",
          "endTime": "2024-01-01T10:15:00Z"
        }
      },
      {
        "systemName": "weatherSystem",
        "interfaceName": "getWeatherData",
        "requestData": {
          "location": "Beijing",
          "fields": "temperature,humidity,wind"
        }
      },
      {
        "systemName": "iotSystem",
        "interfaceName": "getSensorData",
        "requestData": {
          "sensorTypes": ["temperature", "light", "humidity"],
          "timeRange": "15min"
        }
      }
    ],
    "traceId": "data_collection_" + new Date().getTime()
  }'
```

#### 3. 数据处理与保存

```bash
# 处理收集到的数据
curl -X POST http://localhost:8080/unified-scheduler/api/data-processing/transform \
  -H "Content-Type: application/json" \
  -d '{
    "rawData": {
      "powerData": [...],
      "weatherData": {...},
      "iotData": {...}
    },
    "transformScript": "// 综合数据处理\ndata.processedData = {\n  timestamp: new Date(),\n  powerMetrics: data.powerData.transformedData,\n  environmentalFactors: {\n    weather: data.weatherData.weather,\n    sensors: data.iotData.sensors\n  },\n  correlationId: utils.defaultValue(data.correlationId, \"unknown\")\n};\n// 计算功率预测相关指标\ndata.processedData.averagePower = data.processedData.powerMetrics.reduce((sum, item) => sum + item.power, 0) / data.processedData.powerMetrics.length;\ndata.processedData.weatherImpact = data.processedData.environmentalFactors.weather.temperature > 25 ? \"HIGH\" : \"NORMAL\";\nreturn data;",
    "systemName": "powerPredictionSystem",
    "interfaceName": "dataCollection"
  }'
```

#### 4. 发送到消息队列

```bash
# 发送处理后的数据到MQ
curl -X POST http://localhost:8080/unified-scheduler/api/data-processing/send-to-mq \
  -H "Content-Type: application/json" \
  -d '{
    "processedData": {
      "timestamp": "2024-01-01T10:15:00Z",
      "powerMetrics": [...],
      "environmentalFactors": {...},
      "averagePower": 1025.3,
      "weatherImpact": "NORMAL"
    },
    "systemName": "powerPredictionSystem",
    "interfaceName": "dataCollection",
    "traceId": "data_processing_001"
  }'
```

## 监控与运维示例

### 1. 健康检查

```bash
# 检查服务健康状态
curl http://localhost:8080/unified-scheduler/actuator/health

# 检查详细指标
curl http://localhost:8080/unified-scheduler/actuator/metrics
```

### 2. 查看接口调用日志

```bash
# 查询特定系统的调用日志
curl "http://localhost:8080/unified-scheduler/api/interface-logs?systemName=powerSystem&page=0&size=10"

# 查询失败的调用记录
curl "http://localhost:8080/unified-scheduler/api/interface-logs?status=FAILED&startTime=2024-01-01T00:00:00Z"
```

### 3. Token管理

```bash
# 获取系统认证Token
curl -X POST "http://localhost:8080/unified-scheduler/api/interface-call/auth-token/powerSystem"

# 清除Token缓存（强制重新获取）
curl -X DELETE "http://localhost:8080/unified-scheduler/api/interface-call/auth-token/powerSystem"
```

## 高级用法

### 1. 自定义数据转换脚本

```groovy
// 复杂的数据转换示例
import java.text.SimpleDateFormat

// 验证数据完整性
if (!data.result || !data.result.data) {
    throw new RuntimeException("数据格式错误：缺少result.data字段")
}

// 时间格式转换
def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
data.result.data.each { item ->
    if (item.timestamp) {
        item.formattedTime = dateFormat.format(new Date(item.timestamp))
    }
}

// 数据聚合
def summary = [
    totalCount: data.result.data.size(),
    avgValue: data.result.data.sum { it.value ?: 0 } / data.result.data.size(),
    minValue: data.result.data.collect { it.value ?: 0 }.min(),
    maxValue: data.result.data.collect { it.value ?: 0 }.max()
]

// 数据清洗
data.cleanedData = data.result.data.findAll { item ->
    item.value != null && item.value > 0
}.collect { item ->
    [
        id: item.deviceId,
        value: Math.round(item.value * 100) / 100.0, // 保留两位小数
        timestamp: item.timestamp,
        quality: item.quality ?: "UNKNOWN"
    ]
}

data.summary = summary
data.processTime = new Date()

return data
```

### 2. 条件重试策略

```yaml
# 接口配置中的高级重试设置
{
  "retryTimes": 3,
  "retryIntervalSeconds": 5,
  "retryConditions": {
    "httpStatus": [500, 502, 503, 504],
    "errorMessages": ["timeout", "connection refused"],
    "customScript": "response.status >= 500 || response.body.contains('temporary error')"
  }
}
```

### 3. 动态接口配置

```bash
# 根据环境动态修改接口配置
curl -X PUT http://localhost:8080/unified-scheduler/api/interface-config/1 \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "interfaceUrl": "http://backup-server.com/api/data",
    "timeoutSeconds": 60,
    "retryTimes": 5,
    "description": "切换到备用服务器"
  }'
```

这些示例展示了统一调度服务的各种使用场景，从简单的接口调用到复杂的数据处理流程，帮助用户快速上手并充分利用系统的功能。