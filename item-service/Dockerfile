# 使用官方 OpenJDK 17 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY item-service.jar app.jar

# 声明端口
EXPOSE 8081

# 启动应用（包含 JVM 参数）
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/app/app.jar"]