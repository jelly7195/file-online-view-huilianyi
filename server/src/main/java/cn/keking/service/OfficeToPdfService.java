package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import com.sun.star.document.UpdateDocMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author yudian-it
 */
@Slf4j
@Component
public class OfficeToPdfService {

    private final static Logger logger = LoggerFactory.getLogger(OfficeToPdfService.class);

    private static final long CONVERSION_TIMEOUT = 120000;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private OfficePluginManager officePluginManager;

    public void openOfficeToPDF(String inputFilePath, String outputFilePath, FileAttribute fileAttribute) throws OfficeException {
        office2pdf(inputFilePath, outputFilePath, fileAttribute);
    }


    public static void converterFile(File inputFile, String outputFilePath_end, FileAttribute fileAttribute) throws OfficeException {
        File outputFile = new File(outputFilePath_end);
        // 假如目标路径不存在,则新建该路径
        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            logger.error("创建目录【{}】失败，请检查目录权限！",outputFilePath_end);
        }
        LocalConverter.Builder builder;
        Map<String, Object> filterData = new HashMap<>();
        filterData.put("EncryptFile", true);
        if(!ConfigConstants.getOfficePageRange().equals("false")){
//            filterData.put("PageRange", ConfigConstants.getOfficePageRange()); //限制页面
        }
        if(!ConfigConstants.getOfficeWatermark().equals("false")){
//            filterData.put("Watermark", ConfigConstants.getOfficeWatermark());  //水印
        }
        filterData.put("Quality", ConfigConstants.getOfficeQuality()); //图片压缩
        filterData.put("MaxImageResolution", ConfigConstants.getOfficeMaxImageResolution()); //DPI
        if(ConfigConstants.getOfficeExportBookmarks()){
//            filterData.put("ExportBookmarks", true); //导出书签
        }
        if(ConfigConstants.getOfficeExportNotes()){
//            filterData.put("ExportNotes", true); //批注作为PDF的注释
        }
        if(ConfigConstants.getOfficeDocumentOpenPasswords()){
            filterData.put("DocumentOpenPassword", fileAttribute.getFilePassword()); //给PDF添加密码
        }
        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("FilterData", filterData);
        if (StringUtils.isNotBlank(fileAttribute.getFilePassword())) {
            Map<String, Object> loadProperties = new HashMap<>();
            loadProperties.put("Hidden", true);
            loadProperties.put("ReadOnly", true);
            loadProperties.put("UpdateDocMode", UpdateDocMode.NO_UPDATE);
            loadProperties.put("Password", fileAttribute.getFilePassword());
            builder = LocalConverter.builder().loadProperties(loadProperties).storeProperties(customProperties);
        } else {
            builder = LocalConverter.builder().storeProperties(customProperties);
        }
        builder.build().convert(inputFile).to(outputFile).execute();
    }

    public void office2pdf(String inputFilePath, String outputFilePath, FileAttribute fileAttribute) throws OfficeException {
        if (null != inputFilePath) {
            File inputFile = new File(inputFilePath);
            String targetPath = (null == outputFilePath) ? getOutputFilePath(inputFilePath) : outputFilePath;

            if (inputFile.exists()) {
                long startTime = System.currentTimeMillis();
                Future<?> future = executorService.submit(() -> {
                    try {
                        converterFile(inputFile, targetPath, fileAttribute);
                    } catch (OfficeException e) {
                        throw new RuntimeException(e);
                    }
                });

                try {
                    future.get(CONVERSION_TIMEOUT, TimeUnit.MILLISECONDS);
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("文件转换成功，耗时: {}ms", duration);
                    officePluginManager.recordTaskSuccess();
                } catch (TimeoutException e) {
                    future.cancel(true);
                    logger.error("文件转换超时（{}ms），强制终止", CONVERSION_TIMEOUT);
                    officePluginManager.killStuckProcess();
                    throw new OfficeException("转换超时，已强制终止", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("转换被中断");
                    throw new OfficeException("转换被中断", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof OfficeException) {
                        throw (OfficeException) cause;
                    }
                    throw new OfficeException("转换失败", cause);
                }
            }
        }
    }

    public static String getOutputFilePath(String inputFilePath) {
        return inputFilePath.replaceAll("."+ getPostfix(inputFilePath), ".pdf");
    }

    public static String getPostfix(String inputFilePath) {
        return inputFilePath.substring(inputFilePath.lastIndexOf(".") + 1);
    }

}
