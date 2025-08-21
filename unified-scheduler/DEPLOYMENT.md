# 部署指南

## 环境准备

### 基础环境
- JDK 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+
- RabbitMQ 3.8+
- XXL-Job 2.4.0 (可选)

### 数据库初始化

1. **创建数据库**
```sql
CREATE DATABASE unified_scheduler;
CREATE USER scheduler_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE unified_scheduler TO scheduler_user;
```

2. **表结构**
应用启动时会自动创建表结构（使用JPA的DDL自动生成功能）

## 配置文件

### 生产环境配置 (application-prod.yml)

```yaml
# 生产环境配置
server:
  port: 8080

spring:
  application:
    name: unified-scheduler
  
  # 数据库配置
  datasource:
    url: jdbc:postgresql://your-postgres-host:5432/unified_scheduler
    username: scheduler_user
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  # Redis配置
  redis:
    host: ${REDIS_HOST:your-redis-host}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    database: 0
  
  # RabbitMQ配置
  rabbitmq:
    host: ${RABBITMQ_HOST:your-rabbitmq-host}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

# XXL-Job配置
xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN_ADDRESSES}
    executor:
      appname: unified-scheduler-prod
      port: 9999

# 数据处理配置
data-processing:
  persistence:
    api:
      base-url: ${SAVE_API_BASE_URL}

# 日志配置
logging:
  level:
    com.cloudpower.scheduler: INFO
  file:
    path: /data/logs/unified-scheduler
```

## 部署方式

### 1. 传统部署

```bash
# 打包应用
mvn clean package -DskipTests

# 运行应用
java -jar target/unified-scheduler-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### 2. Docker部署

**Dockerfile**
```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app

COPY target/unified-scheduler-1.0.0.jar app.jar

EXPOSE 8080 9999

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**
```yaml
version: '3.8'

services:
  unified-scheduler:
    build: .
    ports:
      - "8080:8080"
      - "9999:9999"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_PASSWORD=your_db_password
      - REDIS_HOST=redis
      - REDIS_PASSWORD=your_redis_password
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_USERNAME=admin
      - RABBITMQ_PASSWORD=your_rabbitmq_password
      - XXL_JOB_ADMIN_ADDRESSES=http://xxl-job:8080/xxl-job-admin
      - SAVE_API_BASE_URL=http://data-service:8081/api
    depends_on:
      - postgres
      - redis
      - rabbitmq
    volumes:
      - ./logs:/data/logs/unified-scheduler

  postgres:
    image: postgres:14
    environment:
      - POSTGRES_DB=unified_scheduler
      - POSTGRES_USER=scheduler_user
      - POSTGRES_PASSWORD=your_db_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    command: redis-server --requirepass your_redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=your_rabbitmq_password
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
```

### 3. Kubernetes部署

**deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: unified-scheduler
spec:
  replicas: 2
  selector:
    matchLabels:
      app: unified-scheduler
  template:
    metadata:
      labels:
        app: unified-scheduler
    spec:
      containers:
      - name: unified-scheduler
        image: your-registry/unified-scheduler:1.0.0
        ports:
        - containerPort: 8080
        - containerPort: 9999
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: scheduler-secrets
              key: db-password
        - name: REDIS_HOST
          value: "redis-service"
        - name: RABBITMQ_HOST
          value: "rabbitmq-service"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /unified-scheduler/actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /unified-scheduler/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: unified-scheduler-service
spec:
  selector:
    app: unified-scheduler
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: xxl-job
    port: 9999
    targetPort: 9999
  type: ClusterIP
```

## 监控配置

### 1. Prometheus监控
```yaml
# prometheus配置
- job_name: 'unified-scheduler'
  static_configs:
  - targets: ['unified-scheduler:8080']
  metrics_path: '/unified-scheduler/actuator/prometheus'
```

### 2. 日志收集
```yaml
# filebeat配置
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /data/logs/unified-scheduler/*.log
  fields:
    service: unified-scheduler
    environment: production
```

## 运维脚本

### 健康检查脚本
```bash
#!/bin/bash
# health-check.sh

ENDPOINT="http://localhost:8080/unified-scheduler/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $ENDPOINT)

if [ $RESPONSE -eq 200 ]; then
    echo "Service is healthy"
    exit 0
else
    echo "Service is unhealthy (HTTP $RESPONSE)"
    exit 1
fi
```

### 备份脚本
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# 备份数据库
pg_dump -h postgres-host -U scheduler_user unified_scheduler > $BACKUP_DIR/database.sql

# 备份配置文件
cp /app/application-prod.yml $BACKUP_DIR/

echo "Backup completed: $BACKUP_DIR"
```

## 性能调优

### JVM参数
```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:G1HeapRegionSize=16m \
     -XX:+UseStringDeduplication \
     -XX:+OptimizeStringConcat \
     -jar unified-scheduler.jar
```

### 数据库连接池调优
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### Redis连接池调优
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
        max-wait: 10000ms
```

## 故障排查

### 常见问题

1. **应用启动失败**
   - 检查数据库连接
   - 验证Redis/RabbitMQ连接
   - 查看端口占用情况

2. **接口调用失败**
   - 检查外部系统网络连通性
   - 验证认证Token是否过期
   - 查看接口调用日志

3. **性能问题**
   - 监控数据库连接池状态
   - 检查Redis缓存命中率
   - 分析JVM内存使用情况

### 日志分析
```bash
# 查看应用日志
tail -f /data/logs/unified-scheduler/application.log

# 查看错误日志
grep ERROR /data/logs/unified-scheduler/application.log

# 查看接口调用统计
grep "Interface call completed" /data/logs/unified-scheduler/application.log | wc -l
```

## 升级指南

### 滚动升级
1. 部署新版本到部分节点
2. 验证新版本功能正常
3. 逐步替换所有节点
4. 验证整体系统功能

### 数据库迁移
```bash
# 备份生产数据库
pg_dump unified_scheduler > backup_before_upgrade.sql

# 应用新版本
java -jar new-version.jar --spring.jpa.hibernate.ddl-auto=update

# 验证数据完整性
```