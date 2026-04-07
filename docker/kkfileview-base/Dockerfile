FROM eclipse-temurin:8-jre-jammy
# 安装必要的包，包括字体工具
RUN apt-get update && apt-get install -y libreoffice fontconfig xfonts-utils && rm -rf /var/lib/apt/lists/*
# 复制字体
ADD fonts/* /usr/share/fonts/kkfonts/
# 安装字体
RUN cd /usr/share/fonts/kkfonts &&\
    mkfontscale &&\
    mkfontdir &&\
    fc-cache -fv