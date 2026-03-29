# 原始版本
# FROM keking/kkfileview-base:4.4.0
# ADD server/target/kkFileView-*.tar.gz /opt/
# ENV KKFILEVIEW_BIN_FOLDER=/opt/kkFileView-4.4.0/bin
# ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.config.location=/opt/kkFileView-4.4.0/config/application.properties","-jar","/opt/kkFileView-4.4.0/bin/kkFileView-4.4.0.jar"]


# hly版本
FROM registry-vpc.huilianyi.com:5000/devops/openjdk:8u151-alp
# RUN echo -e 'https://mirrors.aliyun.com/alpine/v3.13/main/\nhttps://mirrors.aliyun.com/alpine/v3.13/community/' > /etc/apk/repositories
# RUN apk update
RUN apk add  --no-cache libreoffice
ADD server/target/file-online-view.jar /opt/
ENV KKFILEVIEW_BIN_FOLDER /opt/file-online-view
ENTRYPOINT ["java","-jar","/opt/file-online-view.jar"]
