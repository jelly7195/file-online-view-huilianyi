package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.utils.LocalOfficeUtils;
import com.sun.star.document.UpdateDocMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFamily;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.InstalledOfficeManagerHolder;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yudian-it
 */
@Slf4j
@Component
public class OfficeToPdfService {

    private final static Logger logger = LoggerFactory.getLogger(OfficeToPdfService.class);

    public void openOfficeToPDF(String inputFilePath, String outputFilePath, FileAttribute fileAttribute) throws OfficeException {
        office2pdf(inputFilePath, outputFilePath, fileAttribute);
    }


    public static void converterFile(File inputFile, String outputFilePath_end, FileAttribute fileAttribute) throws OfficeException {
        File outputFile = new File(outputFilePath_end);
        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            logger.error("创建目录【{}】失败，请检查目录权限！",outputFilePath_end);
        }
        LocalConverter.Builder builder;
        Map<String, Object> filterData = new HashMap<>();
        filterData.put("EncryptFile", true);
        if(!ConfigConstants.getOfficePageRange().equals("false")){
        }
        if(!ConfigConstants.getOfficeWatermark().equals("false")){
        }
        filterData.put("Quality", ConfigConstants.getOfficeQuality());
        filterData.put("MaxImageResolution", ConfigConstants.getOfficeMaxImageResolution());
        if(ConfigConstants.getOfficeExportBookmarks()){
        }
        if(ConfigConstants.getOfficeExportNotes()){
        }
        if(ConfigConstants.getOfficeDocumentOpenPasswords()){
            filterData.put("DocumentOpenPassword", fileAttribute.getFilePassword());
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

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new OfficeException("文件转换失败，输出文件为空或不存在: " + outputFilePath_end);
        }
    }


    public void office2pdf(String inputFilePath, String outputFilePath, FileAttribute fileAttribute) throws OfficeException {
        if (null != inputFilePath) {
            File inputFile = new File(inputFilePath);
            if (null == outputFilePath) {
                outputFilePath = getOutputFilePath(inputFilePath);
            }
            File outputFile = new File(outputFilePath);
            if (inputFile.exists() && inputFile.length() > 0) {
                try {
                    converterFile(inputFile, outputFilePath, fileAttribute);
                } catch (OfficeException e) {
                    logger.error("文件转换失败: {}", e.getMessage());
                    throw e;
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
