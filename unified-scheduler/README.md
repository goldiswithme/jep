# 统一外部系统接口调度服务

## 项目概述

统一外部系统接口调度服务是一个基于Spring Boot的企业级微服务，专为云端功率预测系统设计，提供对外部系统接口的统一调度、管理和监控功能。该服务支持多种协议，具有高度的可配置性和扩展性，可作为通用组件推广到其他项目。

## 核心特性

### 🚀 多协议支持
- **HTTP/HTTPS**: 标准REST API调用
- **WebSocket**: 实时双向通信
- **MQTT**: 物联网设备通信
- **WebService**: 企业级SOAP服务
- **扩展性**: 支持插件化新协议

### 🔐 智能认证管理
- **自动Token管理**: 自动获取、缓存和刷新认证Token
- **接口编排**: 支持登录→获取Token→业务调用的完整流程
- **认证策略**: 多种认证方式支持
- **缓存机制**: Redis缓存Token，提高性能

### ⏰ 灵活任务调度
- **XXL-Job集成**: 分布式任务调度
- **定时任务**: 支持Cron表达式配置
- **异步调用**: 非阻塞接口调用
- **批量处理**: 支持批量接口调用

### 🔄 强大数据处理
- **脚本引擎**: Groovy/JavaScript数据转换脚本
- **格式转换**: JSON/XML/自定义格式互转
- **数据验证**: 内置数据校验规则
- **处理管道**: 可配置的数据处理流水线

### 📊 数据持久化
- **MQ发送**: 处理后数据发送到RabbitMQ
- **统一保存API**: 通过REST API保存到数据库
- **双写保障**: MQ+API双重保障数据不丢失
- **重试机制**: 自动重试和故障恢复

### 📝 全面监控日志
- **调用日志**: 详细记录每次接口调用
- **性能监控**: 响应时间、成功率统计
- **错误追踪**: 完整的错误信息和堆栈
- **链路追踪**: 支持分布式链路追踪

## 技术栈

- **框架**: Spring Boot 2.7.18
- **Java版本**: JDK 17
- **微服务**: Spring Cloud Alibaba
- **数据库**: PostgreSQL
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **任务调度**: XXL-Job
- **构建工具**: Maven

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+
- RabbitMQ 3.8+

### 安装部署

1. **克隆项目**
```bash
git clone https://github.com/your-repo/unified-scheduler.git
cd unified-scheduler
```

2. **配置数据库**
```sql
CREATE DATABASE unified_scheduler;
```

3. **修改配置文件**
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/unified_scheduler
    username: your_username
    password: your_password
```

4. **启动服务**
```bash
mvn spring-boot:run
```

5. **验证部署**
```bash
curl http://localhost:8080/unified-scheduler/actuator/health
```

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/unified_scheduler
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update
```

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

### RabbitMQ配置
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### XXL-Job配置
```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: unified-scheduler
      port: 9999
```

## API文档

### 接口配置管理

#### 创建接口配置
```http
POST /api/interface-config
Content-Type: application/json

{
  "systemName": "powerSystem",
  "interfaceName": "dataQuery",
  "protocolType": "HTTP",
  "interfaceUrl": "http://external-system/api/data",
  "httpMethod": "POST",
  "timeoutSeconds": 30,
  "retryTimes": 3,
  "authRequired": true,
  "authInterfaceId": 1,
  "tokenCacheKey": "power_system_token",
  "dataTransformScript": "// Groovy script for data transformation\ndata.timestamp = new Date()\nreturn data"
}
```

#### 查询接口配置
```http
GET /api/interface-config/system/powerSystem
```

### 接口调用

#### 执行接口调用
```http
POST /api/interface-call/execute
Content-Type: application/json

{
  "systemName": "powerSystem",
  "interfaceName": "dataQuery",
  "requestData": {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  },
  "traceId": "trace_123456"
}
```

#### 批量接口调用
```http
POST /api/interface-call/execute-batch
Content-Type: application/json

{
  "callRequests": [
    {
      "systemName": "system1",
      "interfaceName": "api1",
      "requestData": {...}
    },
    {
      "systemName": "system2", 
      "interfaceName": "api2",
      "requestData": {...}
    }
  ],
  "traceId": "batch_123456"
}
```

### 数据处理

#### 测试数据转换
```http
POST /api/data-processing/test-transformation
Content-Type: application/json

{
  "sampleData": {
    "temperature": 25.6,
    "humidity": 60.2,
    "timestamp": "2024-01-01T10:00:00Z"
  },
  "transformScript": "data.celsius = data.temperature; data.fahrenheit = data.temperature * 9/5 + 32; return data"
}
```

## 数据转换脚本

### 脚本语法

支持Groovy和JavaScript语法，内置丰富的工具函数：

```groovy
// 格式化日期
data.formattedDate = utils.formatDate(new Date(), 'yyyy-MM-dd HH:mm:ss')

// 设置默认值
data.status = utils.defaultValue(data.status, 'ACTIVE')

// 提取嵌套字段
data.userId = utils.extractField(data, 'user.profile.id')

// 验证数据
if (!utils.validateEmail(data.email)) {
    throw new RuntimeException("Invalid email format")
}

// 转换数组
data.processedItems = data.items.collect { item ->
    [id: item.id, name: item.name.toUpperCase()]
}

return data
```

### 内置工具函数

| 函数名 | 描述 | 示例 |
|--------|------|------|
| `formatDate` | 格式化日期 | `utils.formatDate(date, 'yyyy-MM-dd')` |
| `parseJson` | 解析JSON | `utils.parseJson(jsonString)` |
| `toJson` | 转为JSON | `utils.toJson(object)` |
| `isEmpty` | 检查空值 | `utils.isEmpty(value)` |
| `defaultValue` | 设置默认值 | `utils.defaultValue(value, defaultVal)` |
| `extractField` | 提取字段 | `utils.extractField(data, 'path.to.field')` |
| `validateEmail` | 验证邮箱 | `utils.validateEmail(email)` |
| `validatePhone` | 验证手机号 | `utils.validatePhone(phone)` |

## 任务调度配置

### XXL-Job任务示例

1. **通用接口调用任务**
```
任务名称: genericInterfaceCall
执行器: unified-scheduler
JobHandler: genericInterfaceCallJob
参数: powerSystem,dataQuery,{"startDate":"2024-01-01"}
Cron: 0 0 9 * * ?
```

2. **认证Token刷新任务**
```
任务名称: tokenRefresh
执行器: unified-scheduler
JobHandler: tokenRefreshJob
参数: powerSystem,weatherSystem,iotSystem
Cron: 0 0 */2 * * ?
```

3. **数据同步任务**
```
任务名称: dataSync
执行器: unified-scheduler
JobHandler: dataSyncJob
参数: 
Cron: 0 */30 * * * ?
```

## 监控和运维

### 健康检查
```bash
curl http://localhost:8080/unified-scheduler/actuator/health
```

### 指标监控
```bash
curl http://localhost:8080/unified-scheduler/actuator/metrics
```

### 日志查看
```bash
tail -f /data/logs/unified-scheduler/application.log
```

### 性能调优

1. **连接池配置**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

2. **HTTP客户端配置**
```yaml
external-systems:
  http:
    connect-timeout: 30000
    read-timeout: 60000
    connection-pool-size: 100
```

3. **缓存配置**
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
```

## 扩展开发

### 新增协议支持

1. **创建协议处理器**
```java
@Component
public class MqttProtocolHandler implements ProtocolHandler {
    @Override
    public ProtocolType getSupportedType() {
        return ProtocolType.MQTT;
    }
    
    @Override
    public Map<String, Object> execute(ExternalInterfaceConfig config, 
                                     Map<String, Object> requestData) {
        // MQTT调用实现
    }
}
```

2. **注册协议处理器**
```java
@Configuration
public class ProtocolConfig {
    @Bean
    public ProtocolHandlerRegistry protocolHandlerRegistry() {
        return new ProtocolHandlerRegistry();
    }
}
```

### 自定义数据处理器

```java
@Component
public class CustomDataProcessor implements DataProcessor {
    @Override
    public Map<String, Object> process(Map<String, Object> data, 
                                     ProcessingContext context) {
        // 自定义处理逻辑
        return processedData;
    }
}
```

## 常见问题

### Q: 如何处理接口调用超时？
A: 在接口配置中设置`timeoutSeconds`参数，系统会自动处理超时和重试。

### Q: 如何调试数据转换脚本？
A: 使用`/api/data-processing/test-transformation`接口测试脚本。

### Q: 如何查看接口调用日志？
A: 所有调用都记录在`interface_call_log`表中，可通过管理界面查看。

### Q: 如何添加新的外部系统？
A: 通过`/api/interface-config`接口创建新的接口配置即可。

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交代码
4. 发起Pull Request

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 联系方式

- 项目维护者: 云端功率预测团队
- 邮箱: team@cloudpower.com
- 技术支持: support@cloudpower.com