# 使用官方的 OpenJDK 8 镜像作为基础镜像
FROM openjdk:8-jdk-slim

# 设置工作目录
WORKDIR /app