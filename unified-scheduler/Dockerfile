FROM openjdk:17-jre-slim

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/unified-scheduler-1.0.0.jar app.jar

# 创建日志目录
RUN mkdir -p /data/logs/unified-scheduler

# 添加用户（安全考虑）
RUN addgroup --system appgroup && adduser --system --group appuser
RUN chown -R appuser:appgroup /app /data
USER appuser

# 暴露端口
EXPOSE 8080 9999

# JVM参数配置
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+OptimizeStringConcat"

# 启动命令
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar