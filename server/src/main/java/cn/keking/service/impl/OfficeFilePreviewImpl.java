package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.OfficeToPdfService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.OfficeUtils;
import cn.keking.utils.WebUtils;
import cn.keking.web.filter.BaseUrlFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.EncryptedDocumentException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jodconverter.core.office.OfficeException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :处理office文件
 */
@Slf4j
@Service
public class OfficeFilePreviewImpl implements FilePreview {

    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    public static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";
    private static final String OFFICE_PASSWORD_MSG = "password";

    private final FileHandlerService fileHandlerService;
    private final OfficeToPdfService officeToPdfService;
    private final OtherFilePreviewImpl otherFilePreview;

    public OfficeFilePreviewImpl(FileHandlerService fileHandlerService, OfficeToPdfService officeToPdfService, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.officeToPdfService = officeToPdfService;
        this.otherFilePreview = otherFilePreview;
    }

    /**
     * 针对不同文件相同文件名导致的预览问题
     * 比如url  A: AAA/123.XLSX A: BBB/123.XLSX
     * 应该返回文件名 AAA123.XLSX  BBB123.XLSX、
     * 这样缓存和本地文件就不会取同一个文件
     * @param url
     * @param fileName
     * @return String
     * @author xin.zhou
     * @date 2024/7/9 14:58
     */
    private String getDistinctFileName(String url, String fileName) {
        if(StringUtils.isEmpty(url) || StringUtils.isEmpty(fileName)){
            return fileName;
        }
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.info("decode failed: {}", ExceptionUtils.getMessage(e));
        }
        if(url.indexOf(fileName) != -1 && url.indexOf(fileName) > 14){
//            return url.substring(url.indexOf(fileName)-13,url.indexOf(fileName)-1) + fileName;
            fileName = url.substring(url.indexOf(fileName)-13,url.indexOf(fileName)-1) + fileName;
            return fileName.replace("/","");
        }
        return fileName;

    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = fileAttribute.getOfficePreviewType();
        boolean userToken = fileAttribute.getUsePasswordCache();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String suffix = fileAttribute.getSuffix();  //获取文件后缀
        String fileName = fileAttribute.getName(); //获取文件原始名称
        String filePassword = fileAttribute.getFilePassword(); //获取密码
        boolean forceUpdatedCache=fileAttribute.forceUpdatedCache();  //是否启用强制更新命令
        boolean isHtmlView = fileAttribute.isHtmlView();  //xlsx  转换成html
        String cacheName = fileAttribute.getCacheName();  //转换后的文件名
        String outFilePath = fileAttribute.getOutFilePath();  //转换后生成文件的路径
        if (!officePreviewType.equalsIgnoreCase("html")) {
            if (ConfigConstants.getOfficeTypeWeb() .equalsIgnoreCase("web")) {
                //xlsx已经优化,替换了前端渲染组件，见 excel.ftl
                if (suffix.equalsIgnoreCase("xlsx")) {
                    model.addAttribute("pdfUrl", KkFileUtils.htmlEscape(url)); //特殊符号处理
                    return XLSX_FILE_PREVIEW_PAGE;
                }
                if (suffix.equalsIgnoreCase("csv")) {
                    model.addAttribute("csvUrl", KkFileUtils.htmlEscape(url));
                    return CSV_FILE_PREVIEW_PAGE;
                }
            }
        }
        if (forceUpdatedCache|| !fileHandlerService.listConvertedFiles().containsKey(cacheName) || !ConfigConstants.isCacheEnabled()) {
        // 下载远程文件到本地，如果文件在本地已存在不会重复下载
        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
        if (response.isFailure()) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
        }
            String filePath = response.getContent();
            boolean  isPwdProtectedOffice =  OfficeUtils.isPwdProtected(filePath);    // 判断是否加密文件
            if (isPwdProtectedOffice && !StringUtils.hasLength(filePassword)) {
                // 加密文件需要密码
                model.addAttribute("needFilePassword", true);
                return EXEL_FILE_PREVIEW_PAGE;
            } else {
                if (StringUtils.hasText(outFilePath)) {
                    try {
                        long current = System.currentTimeMillis();
                        log.info("===>转换文件开始,path={}",filePath);
                        officeToPdfService.openOfficeToPDF(filePath, outFilePath, fileAttribute);
                        log.info("===>转换文件结束,耗时:{}毫秒,path={}",System.currentTimeMillis() - current,outFilePath);
                    } catch (OfficeException e) {
                        if (isPwdProtectedOffice && !OfficeUtils.isCompatible(filePath, filePassword)) {
                            // 加密文件密码错误，提示重新输入
                            model.addAttribute("needFilePassword", true);
                            model.addAttribute("filePasswordError", true);
                            return EXEL_FILE_PREVIEW_PAGE;
                        }
                        log.error("转换文件失败！exception：", e);
                        return otherFilePreview.notSupportedFile(model, fileAttribute, "抱歉，该文件转换失败，请联系管理员。"+e.getMessage());
                    }
                    if (isHtmlView) {
                        // 对转换后的文件进行操作(改变编码方式)
                        fileHandlerService.doActionConvertedFile(outFilePath);
                    }
                    //是否保留OFFICE源文件
                    if (!fileAttribute.isCompressFile() && ConfigConstants.getDeleteSourceFile()) {
                        KkFileUtils.deleteFileByPath(filePath);
                    }
                    if (userToken || !isPwdProtectedOffice) {
                        // 加入缓存
                        fileHandlerService.addConvertedFile(cacheName, fileHandlerService.getRelativePath(outFilePath));
                    }
                }
            }

        }else {
            log.info("文件已存在，直接读取缓存文件:{}",cacheName);
        }
        if (!isHtmlView && baseUrl != null && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType))) {
            return getPreviewType(model, fileAttribute, officePreviewType, cacheName, outFilePath, fileHandlerService, OFFICE_PREVIEW_TYPE_IMAGE, otherFilePreview);
        }
        model.addAttribute("pdfUrl", WebUtils.encodeFileName(cacheName));  //输出转义文件名 方便url识别
        return isHtmlView ? EXEL_FILE_PREVIEW_PAGE : PDF_FILE_PREVIEW_PAGE;
    }

    static String getPreviewType(Model model, FileAttribute fileAttribute, String officePreviewType, String pdfName, String outFilePath, FileHandlerService fileHandlerService, String officePreviewTypeImage, OtherFilePreviewImpl otherFilePreview) {
        String suffix = fileAttribute.getSuffix();
        boolean isPPT = suffix.equalsIgnoreCase("ppt") || suffix.equalsIgnoreCase("pptx");
        List<String> imageUrls = null;
        try {
            imageUrls =  fileHandlerService.pdf2jpg(outFilePath,outFilePath, pdfName, fileAttribute);
        } catch (Exception e) {
            Throwable[] throwableArray = ExceptionUtils.getThrowables(e);
            for (Throwable throwable : throwableArray) {
                if (throwable instanceof IOException || throwable instanceof EncryptedDocumentException) {
                    if (e.getMessage().toLowerCase().contains(OFFICE_PASSWORD_MSG)) {
                        model.addAttribute("needFilePassword", true);
                        return EXEL_FILE_PREVIEW_PAGE;
                    }
                }
            }
        }
        if (imageUrls == null || imageUrls.size() < 1) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, "office转图片异常，请联系管理员");
        }
        model.addAttribute("imgUrls", imageUrls);
        model.addAttribute("currentUrl", imageUrls.get(0));
        if (officePreviewTypeImage.equals(officePreviewType)) {
            // PPT 图片模式使用专用预览页面
            return (isPPT ? PPT_FILE_PREVIEW_PAGE : OFFICE_PICTURE_FILE_PREVIEW_PAGE);
        } else {
            return PICTURE_FILE_PREVIEW_PAGE;
        }
    }

    /**
     * office文件转换pdf，并返回文件url
     *
     * @param url
     * @param fileAttribute
     * @return String
     * @author xin.zhou
     * @date 2024/10/23 14:56
     */
    public String officeToPDF(@NonNull String url, @NonNull FileAttribute fileAttribute){
        log.info("office to pdf start");
        String suffix = fileAttribute.getSuffix();
        String fileName = getDistinctFileName(url, fileAttribute.getName());
        //excel要转html  其余都是转pdf
        boolean isHtml = suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx") || suffix.equalsIgnoreCase("csv") || suffix.equalsIgnoreCase("xlsm") || suffix.equalsIgnoreCase("xlt") || suffix.equalsIgnoreCase("xltm") || suffix.equalsIgnoreCase("et") || suffix.equalsIgnoreCase("ett") || suffix.equalsIgnoreCase("xlam");
        String cacheFileName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + (isHtml ? "html" : "pdf");
        //转换后文件地址
        String outFilePath = ConfigConstants.getFileDir() + cacheFileName;

        if (!fileHandlerService.listConvertedFiles().containsKey(cacheFileName) || !ConfigConstants.isCacheEnabled()) {
            // 下载远程文件到本地，如果文件在本地已存在不会重复下载
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
            if (response.isFailure()) {
                throw new RuntimeException(response.getMsg());
            }
            String filePath = response.getContent();
            boolean isCached = false;
            boolean isUseCached = false;
            if (ConfigConstants.isCacheEnabled()) {
                // 全局开启缓存
                isUseCached = true;
                if (fileHandlerService.listConvertedFiles().containsKey(cacheFileName)) {
                    // 存在缓存
                    isCached = true;
                }
            }
            if (!isCached && StringUtils.hasText(outFilePath)) {
                // 没有缓存执行转换逻辑
                try {
                    log.info("开始转换文件{}",filePath);
                    officeToPdfService.openOfficeToPDF(filePath, outFilePath, fileAttribute);
                    log.info("转换文件结束{}",outFilePath);
                } catch (OfficeException e) {
                    log.error("转换文件失败！exception：{}", ExceptionUtils.getStackTrace(e));
                    throw new RuntimeException("抱歉，该文件转换失败，请联系管理员。"+ExceptionUtils.getStackTrace(e));
                }
                if (isHtml) {
                    // 对转换后的文件进行操作(改变编码方式)
                    fileHandlerService.doActionConvertedFile(outFilePath);
                }
                if (isUseCached) {
                    // 加入缓存
                    fileHandlerService.addConvertedFile(cacheFileName, fileHandlerService.getRelativePath(outFilePath));
                }
            }
        }else{
            log.info("命中缓存: {}",cacheFileName);
        }
        //模拟前端返回最终url see pdf.ftl#<#assign finalUrl="${baseUrl}${pdfUrl}">  15行
        cacheFileName = URLEncoder.encode(cacheFileName).replaceAll("\\+", "%20");
        String baseUrl = BaseUrlFilter.getBaseUrl();
        return baseUrl + cacheFileName;
    }

}
