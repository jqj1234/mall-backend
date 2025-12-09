# 使用官方 OpenJDK 17 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY search-service.jar app.jar

# 声明端口
EXPOSE 8086

# 启动应用（包含 JVM 参数）
ENTRYPOINT ["java", "-jar", "/app/app.jar"]