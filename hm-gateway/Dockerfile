# 使用官方 OpenJDK 17 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件（建议使用 COPY 而非 ADD，除非需要解压 tar）
COPY hm-gateway.jar app.jar

# 声明端口（文档作用，实际由运行时绑定）
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
