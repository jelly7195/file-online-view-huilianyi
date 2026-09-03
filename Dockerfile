# 基础镜像
FROM kkfileview-base:latest

# 添加应用jar
ADD server/target/kkFileView-4.4.0.jar /opt/

# 环境变量配置
ENV KKFILEVIEW_BIN_FOLDER=/opt/kkFileView-4.4.0/bin
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV OOO_FORCE_DESKTOP=0
ENV SAL_ENABLE_FILE_LOCKING=0
ENV SAL_NO_MULTI_JVM_HEAP_SIZE=1

# 健康检查（让Docker自动检测并重启）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD curl -f http://localhost:8012/kkview/health || exit 1

# 直接运行 java
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/kkFileView-4.4.0.jar"]
