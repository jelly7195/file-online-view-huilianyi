#!/bin/bash
# 检查root权限
if [ $EUID -ne 0 ]; then
   echo "请用root权限执行：sudo $0"
   exit 1
fi

# 安装基础依赖（含你缺的cups-libs）
yum install -y wget cups-libs libSM.x86_64 libXrender.x86_64 libXext.x86_64 cairo libXinerama libXrandr libXtst fontconfig freetype glib2 pango libicu mesa-libGL

# 下载并安装LibreOffice
cd /tmp
wget -cO LibreOffice_7_rpm.tar.gz https://downloadarchive.documentfoundation.org/libreoffice/old/7.5.3.2/rpm/x86_64/LibreOffice_7.5.3.2_Linux_x86-64_rpm.tar.gz
tar -zxf LibreOffice_7_rpm.tar.gz
cd LibreOffice_7.5.3.2_Linux_x86-64_rpm/RPMS
yum localinstall -y *.rpm

echo "LibreOffice安装完成"