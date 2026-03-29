package cn.keking.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Office工具类
 *
 * @author ylyue
 * @since 2022/7/5
 */
public class OfficeUtils {

    private static final String POI_INVALID_PASSWORD_MSG = "password";

    /**
     * 判断office（word,excel,ppt）文件是否受密码保护
     *
     * @param path office文件路径
     * @return 是否受密码保护
     */
    public static boolean isPwdProtected(String path) {
        //优化了原方法（存在内存问题）这里资源放在try
        try (InputStream propStream = Files.newInputStream(Paths.get(path))) {
            // 使用try-with-resources确保提取器被正确关闭
            try (POITextExtractor extractor = ExtractorFactory.createExtractor(propStream)) {
                // 只需要创建提取器，不需要实际使用
                return false;
            }
        } catch (IOException | EncryptedDocumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains(POI_INVALID_PASSWORD_MSG)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            // 简化异常处理，避免创建大量Throwable数组
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains(POI_INVALID_PASSWORD_MSG)) {
                return true;
            }
            return false;
        }
    }
    /**
     * 判断office文件是否可打开（兼容）
     *
     * @param path     office文件路径
     * @param password 文件密码
     * @return 是否可打开（兼容）
     */
    public static synchronized boolean isCompatible(String path, String password) {
        InputStream propStream = null;
        try {
            propStream = Files.newInputStream(Paths.get(path));
            Biff8EncryptionKey.setCurrentUserPassword(password);
            ExtractorFactory.createExtractor(propStream);
        } catch (Exception e) {
            return false;
        } finally {
            Biff8EncryptionKey.setCurrentUserPassword(null);
            if(propStream!=null) {//如果文件输入流不是null
                try {
                    propStream.close();//关闭文件输入流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

}