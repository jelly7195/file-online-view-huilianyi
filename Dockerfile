# 原始版本
# FROM keking/kkfileview-base:4.4.0
# ADD server/target/kkFileView-*.tar.gz /opt/
# ENV KKFILEVIEW_BIN_FOLDER=/opt/kkFileView-4.4.0/bin
# ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.config.location=/opt/kkFileView-4.4.0/config/application.properties","-jar","/opt/kkFileView-4.4.0/bin/kkFileView-4.4.0.jar"]

FROM kkfileview-base:latest
ADD server/target/kkFileView-4.4.0.jar /opt/
ENV KKFILEVIEW_BIN_FOLDER=/opt/kkFileView-4.4.0/bin
ENTRYPOINT ["java","-jar","/opt/kkFileView-4.4.0.jar"]
