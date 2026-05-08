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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong lastSuccessTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    @Value("${office.plugin.server.ports:2001,2002}")
    private String serverPorts;

    @Value("${office.plugin.task.timeout:5m}")
    private String timeOut;

    @Value("${office.plugin.task.taskexecutiontimeout:5m}")
    private String taskExecutionTimeout;

    @Value("${office.plugin.task.maxtasksperprocess:5}")
    private int maxTasksPerProcess;

    @Value("${office.plugin.health-check.interval:60000}")
    private long healthCheckInterval;

    @Value("${office.plugin.max-idle-time:300000}")
    private long maxIdleTime;

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
            logger.info("LibreOffice启动成功");
        } catch (Exception e) {
            logger.error("启动office组件失败，请检查office组件是否可用");
            throw e;
        }
    }

    public void recordTaskSuccess() {
        lastSuccessTime.set(System.currentTimeMillis());
        consecutiveFailures.set(0);
    }

    public void recordTaskFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        logger.warn("转换任务失败，连续失败次数: {}", failures);
        
        if (failures >= 3) {
            logger.error("连续失败{}次，强制重启LibreOffice进程", failures);
            restartOfficeManager();
            consecutiveFailures.set(0);
        }
    }

    public void killStuckProcess() {
        logger.warn("检测到卡死进程，开始强制清理...");
        try {
            killProcess();
            if (officeManager != null && officeManager.isRunning()) {
                OfficeUtils.stopQuietly(officeManager);
            }
            InstalledOfficeManagerHolder.setInstance(null);
            logger.info("卡死进程已清理");
        } catch (Exception e) {
            logger.error("清理卡死进程失败", e);
        }
        restartOfficeManager();
    }

    @Scheduled(fixedRateString = "${office.plugin.health-check.interval:60000}")
    public void healthCheck() {
        long now = System.currentTimeMillis();
        long idleTime = now - lastSuccessTime.get();
        
        if (idleTime > maxIdleTime && officeManager != null) {
            try {
                if (!officeManager.isRunning()) {
                    logger.warn("LibreOffice进程已停止但服务未响应，尝试重启...");
                    restartOfficeManager();
                } else {
                    Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps aux | grep soffice | grep -v grep | wc -l"});
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = p.getInputStream();
                    byte[] b = new byte[256];
                    while (is.read(b) > 0) {
                        baos.write(b);
                    }
                    String count = baos.toString().trim();
                    if ("0".equals(count) || "".equals(count)) {
                        logger.error("LibreOffice进程不存在但管理器显示运行中，触发容器重启");
                        System.exit(1);
                    }
                }
            } catch (Exception e) {
                logger.error("健康检查异常", e);
            }
        }
    }

    @Scheduled(cron = "${office.restart.cron:0 0 2 * * ?}")
    public void restartOfficeManager() {
        logger.info("开始执行LibreOffice定时重启任务");
        try {
            if (null != officeManager && officeManager.isRunning()) {
                logger.info("停止当前Office进程");
                OfficeUtils.stopQuietly(officeManager);
                InstalledOfficeManagerHolder.setInstance(null);
            }
            killProcess();
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
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps -ef | grep soffice.bin" + " |grep -v grep | wc -l"});
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

    public boolean isHealthy() {
        return officeManager != null && officeManager.isRunning();
    }
}
