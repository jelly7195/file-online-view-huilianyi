package cn.keking.service.impl;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.OfficeToPdfService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.KkFileUtils;
import cn.keking.web.filter.BaseUrlFilter;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.office.OfficeException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Slf4j
@Service
public class ExcelFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final OfficeToPdfService officeToPdfService;
    private final OtherFilePreviewImpl otherFilePreview;



    public ExcelFilePreviewImpl(FileHandlerService fileHandlerService,OfficeToPdfService officeToPdfService,OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.officeToPdfService = officeToPdfService;
        this.otherFilePreview = otherFilePreview;
    }


    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 预览Type，参数传了就取参数的，没传取系统默认
//        String officePreviewType = fileAttribute.getOfficePreviewType();
//        boolean userToken = fileAttribute.getUsePasswordCache();
        String baseUrl = BaseUrlFilter.getBaseUrl();
//        String suffix = fileAttribute.getSuffix();  //获取文件后缀
        String fileName = fileAttribute.getName(); //获取文件原始名称
//        String filePassword = fileAttribute.getFilePassword(); //获取密码
//        boolean forceUpdatedCache = fileAttribute.forceUpdatedCache();  //是否启用强制更新命令
//        boolean isHtmlView = fileAttribute.isHtmlView();  //xlsx  转换成html
        String cacheName = fileAttribute.getCacheName();  //转换后的文件名
        String outFilePath = fileAttribute.getOutFilePath();  //转换后生成文件的路径

        // 下载远程文件到本地，如果文件在本地已存在不会重复下载
        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
//        if (response.isFailure()) {
//            return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
//        }

        //如果文件超过5M,则不支持预览
        // if(response.getSize() > 3 * 1024 * 1024){
        //     return otherFilePreview.notSupportedFile(model, fileAttribute, "文件大小超过3M,不支持预览");
        // }

        String filePath = response.getContent();

        //如果是xls，先转xlsx 再预览
        if("xls".equals(fileAttribute.getSuffix())){
            if(fileHandlerService.listConvertedFiles().containsKey(fileAttribute.getCacheName())){
                url = fileHandlerService.listConvertedFiles().get(fileAttribute.getCacheName());
                model.addAttribute("pdfUrl", KkFileUtils.htmlEscape(url)); //特殊符号处理
                return "excel";
            }
            try {
                long current = System.currentTimeMillis();
                log.info("===>转换文件开始,path={}", filePath);
                officeToPdfService.openOfficeToPDF(filePath, outFilePath, fileAttribute);
                log.info("===>转换文件结束,耗时:{}毫秒,path={}", System.currentTimeMillis() - current, outFilePath);

                // 加入缓存
                url = baseUrl + fileHandlerService.getRelativePath(outFilePath);
                fileHandlerService.addConvertedFile(cacheName, url);
            } catch (OfficeException e) {
                log.error("===>转换文件异常,path={}", filePath, e);
            }
        }



        model.addAttribute("pdfUrl", KkFileUtils.htmlEscape(url)); //特殊符号处理

        return "excel";
    }

}
