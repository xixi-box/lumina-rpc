# ============================================================
# Java 模块多阶段构建 Dockerfile
# 适用于: lumina-control-plane, lumina-sample-*
# ============================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 复制 pom.xml 和源码
COPY pom.xml .
COPY lumina-rpc-protocol/pom.xml lumina-rpc-protocol/
COPY lumina-rpc-core/pom.xml lumina-rpc-core/
COPY lumina-control-plane/pom.xml lumina-control-plane/
COPY lumina-sample-engine/pom.xml lumina-sample-engine/
COPY lumina-sample-radar/pom.xml lumina-sample-radar/
COPY lumina-sample-command/pom.xml lumina-sample-command/

# 下载依赖（缓存优化）
RUN mvn dependency:go-offline -B

# 复制源码
COPY lumina-rpc-protocol/src lumina-rpc-protocol/src
COPY lumina-rpc-core/src lumina-rpc-core/src
COPY lumina-control-plane/src lumina-control-plane/src
COPY lumina-sample-engine/src lumina-sample-engine/src
COPY lumina-sample-radar/src lumina-sample-radar/src
COPY lumina-sample-command/src lumina-sample-command/src

# 构建指定模块（根据 TARGET_MODULE 参数）
ARG TARGET_MODULE
RUN mvn clean package -pl ${TARGET_MODULE} -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S lumina && adduser -S lumina -G lumina

# 复制 jar 文件
ARG TARGET_MODULE
COPY --from=builder /build/${TARGET_MODULE}/target/*.jar app.jar

# 设置权限
RUN chown -R lumina:lumina /app
USER lumina

# 环境变量
ENV JAVA_OPTS="-Xms128m -Xmx256m"

# 暴露端口（根据模块不同）
EXPOSE 8080 20880 20881 20882

# 配置目录（挂载 application.yml）
RUN mkdir -p /app/config

# 默认入口（可被 docker-compose 覆盖）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=/app/config/"]