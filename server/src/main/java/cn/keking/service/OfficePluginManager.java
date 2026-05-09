package cn.keking.service;

import cn.keking.utils.LocalOfficeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.office.InstalledOfficeManagerHolder;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.core.office.OfficeUtils;
import org.jodconverter.core.util.OSUtils;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建文件转换器
 *
 * @author chenjh
 * @since 2022-12-15
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OfficePluginManager {

    private final Logger logger = LoggerFactory.getLogger(OfficePluginManager.class);

    private LocalOfficeManager officeManager;

    @Value("${office.plugin.server.ports:2001,2002}")
    private String serverPorts;

    @Value("${office.plugin.task.timeout:5m}")
    private String timeOut;

    @Value("${office.plugin.task.taskexecutiontimeout:5m}")
    private String taskExecutionTimeout;

    @Value("${office.plugin.task.maxtasksperprocess:5}")
    private int maxTasksPerProcess;

    @Autowired
    private LocalOfficeUtils localOfficeUtils;

    /**
     * 启动Office组件进程
     */
    @PostConstruct
    public void startOfficeManager() throws OfficeException {
        File officeHome = localOfficeUtils.getDefaultOfficeHome();
        if (officeHome == null) {
            logger.error("未检测到office路径,请确认'office.home'配置是否有误");
            return;
//            throw new RuntimeException("找不到office组件，请确认'office.home'配置是否有误");
        }
        boolean killOffice = killProcess();
        if (killOffice) {
            logger.warn("检测到有正在运行的office进程，已自动结束该进程");
        }
        try {
            String[] portsString = serverPorts.split(",");
            int[] ports = Arrays.stream(portsString).mapToInt(Integer::parseInt).toArray();
            long timeout = DurationStyle.detectAndParse(timeOut).toMillis();
            long taskexecutiontimeout = DurationStyle.detectAndParse(taskExecutionTimeout).toMillis();
            officeManager = LocalOfficeManager.builder()
                    .officeHome(officeHome)
                    .portNumbers(ports)
                    .taskExecutionTimeout(timeout)
                    .processTimeout(timeout)
                    .maxTasksPerProcess(maxTasksPerProcess)
                    .taskExecutionTimeout(taskexecutiontimeout)
                    .build();
            officeManager.start();
            InstalledOfficeManagerHolder.setInstance(officeManager);
        } catch (Exception e) {
            logger.error("启动office组件失败，请检查office组件是否可用");
            throw e;
        }
    }

    /**
     * LibreOffice定时重启任务 每天2点重启
     */
    @Scheduled(cron = "${office.restart.cron:0 0 2 * * ?}")
    public void restartOfficeManager() {
        logger.info("开始执行LibreOffice定时重启任务");
        try {
            // 停止当前Office进程
            if (null != officeManager && officeManager.isRunning()) {
                logger.info("停止当前Office进程");
                OfficeUtils.stopQuietly(officeManager);
                InstalledOfficeManagerHolder.setInstance(null);
            }

            // 确保所有进程都已结束
            killProcess();

            // 重新启动Office进程
            logger.info("重新启动Office进程");
            startOfficeManager();
            logger.info("LibreOffice定时重启任务执行完成");
        } catch (Exception e) {
            logger.error("LibreOffice定时重启任务执行失败", e);
        }
    }



    private boolean killProcess() {
        boolean flag = false;
        try {
            if (OSUtils.IS_OS_WINDOWS) {
                Process p = Runtime.getRuntime().exec("cmd /c tasklist ");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream os = p.getInputStream();
                byte[] b = new byte[256];
                while (os.read(b) > 0) {
                    baos.write(b);
                }
                String s = baos.toString();
                if (s.contains("soffice.bin")) {
                    Runtime.getRuntime().exec("taskkill /im " + "soffice.bin" + " /f");
                    flag = true;
                }
            } else if (OSUtils.IS_OS_MAC || OSUtils.IS_OS_MAC_OSX) {
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps -ef | grep " + "soffice.bin"});
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream os = p.getInputStream();
                byte[] b = new byte[256];
                while (os.read(b) > 0) {
                    baos.write(b);
                }
                String s = baos.toString();
                if (StringUtils.ordinalIndexOf(s, "soffice.bin", 3) > 0) {
                    String[] cmd = {"sh", "-c", "kill -15 `ps -ef|grep " + "soffice.bin" + "|awk 'NR==1{print $2}'`"};
                    Runtime.getRuntime().exec(cmd);
                    flag = true;
                }
            } else {
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps -ef | grep " + "soffice.bin" + " |grep -v grep | wc -l"});
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream os = p.getInputStream();
                byte[] b = new byte[256];
                while (os.read(b) > 0) {
                    baos.write(b);
                }
                String s = baos.toString();
                if (!s.startsWith("0")) {
                    String[] cmd = {"sh", "-c", "ps -ef | grep soffice.bin | grep -v grep | awk '{print \"kill -9 \"$2}' | sh"};
                    Runtime.getRuntime().exec(cmd);
                    flag = true;
                }
            }
        } catch (IOException e) {
            logger.error("检测office进程异常", e);
        }
        return flag;
    }

    @PreDestroy
    public void destroyOfficeManager() {
        if (null != officeManager && officeManager.isRunning()) {
            logger.info("Shutting down office process");
            OfficeUtils.stopQuietly(officeManager);
        }
    }



    public static void main(String[] args) {
//        File officeHome = localOfficeUtils.getDefaultOfficeHome();
        // 1. 启动LibreOffice管理器（实际项目中建议做成单例进程池）
        OfficeManager officeManager = LocalOfficeManager.builder().build();
        try {
            officeManager.start();

            // 2. 定义源文件和输出文件
//            File inputExcel = new File("/Users/lichao/IdeaProjects/file-online-preview/server/src/main/file/demo/hmsh202510管理费用.xlsx"); // 你的Excel文件
            File inputExcel = new File("/Users/lichao/Downloads/ACC_BOOK004_003_1000000_2025-02.xls"); // 你的Excel文件
            File outputHtml = new File("/Users/lichao/IdeaProjects/file-online-preview/server/src/main/file/test.xlsx"); // 输出的HTML文件

            // 3. 核心：设置HTML转换参数，强制保留Excel样式
            Map<String, Object> htmlProperties = new HashMap<>();
            // 保留单元格格式、字体、颜色、合并单元格
//            htmlProperties.put("FilterName", "HTML (Calc)");
//            htmlProperties.put("HTML_Export_CreateIndex", false);
//            htmlProperties.put("HTML_Export_Images", true); // 保留Excel中的图片
//            htmlProperties.put("HTML_Export_Frame", false); // 禁用框架，适配前端渲染
            htmlProperties.put("HTML_Export_Use_CSS", true); // 用CSS而非内联样式，便于前端调整
            // 指定只转换当前sheet（关键参数！）
//            htmlProperties.put("Sheet", "sheetName");


            // 4. 执行转换
            LocalConverter.builder()
                    .officeManager(officeManager)
//                    .storeProperties(htmlProperties)
                    .build()
                   .convert(inputExcel)
                    .to(outputHtml)
//                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.XLSX)
//                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.HTML)
//                    .withProperties(htmlProperties) // 传入样式保留参数
                    .execute();

            System.out.println("Excel转HTML完成，样式已完整保留");

            // 输出文件大小信息
            long fileSizeBytes = outputHtml.length();
            double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            System.out.println("输出文件大小: " + String.format("%.2f", fileSizeMB) + " MB");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}