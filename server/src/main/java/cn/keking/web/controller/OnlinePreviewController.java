package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import cn.keking.service.cache.CacheService;
import cn.keking.service.impl.OfficeFilePreviewImpl;
import cn.keking.service.impl.OtherFilePreviewImpl;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.PdfSafeUtil;
import cn.keking.utils.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensagres.xdocreport.core.io.IOUtils;
import io.mola.galimatias.GalimatiasParseException;
import java.net.URLDecoder;

import lombok.NonNull;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.keking.service.FilePreview.PICTURE_FILE_PREVIEW_PAGE;

/**
 * @author yudian-it
 */
@Controller
public class OnlinePreviewController {

    public static final String BASE64_DECODE_ERROR_MSG = "Base64解码失败，请检查你的 %s 是否采用 Base64 + urlEncode 双重编码了！";
    private final Logger logger = LoggerFactory.getLogger(OnlinePreviewController.class);

    private final FilePreviewFactory previewFactory;
    private final CacheService cacheService;
    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static  final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    private static final ObjectMapper mapper = new ObjectMapper();

    public OnlinePreviewController(FilePreviewFactory filePreviewFactory, FileHandlerService fileHandlerService, CacheService cacheService, OtherFilePreviewImpl otherFilePreview) {
        this.previewFactory = filePreviewFactory;
        this.fileHandlerService = fileHandlerService;
        this.cacheService = cacheService;
        this.otherFilePreview = otherFilePreview;
    }

    @GetMapping( "/onlinePreview")
    public String onlinePreview(String url, Model model, HttpServletRequest req) {

        String fileUrl;
        try {
            fileUrl = WebUtils.decodeUrl(url);
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
//        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(WebUtils.decodeUrlFileName(fileUrl),req);  //这里不在进行URL 处理了
        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl,req);  //这里不在进行URL 处理了
        model.addAttribute("file", fileAttribute);
        FilePreview filePreview = previewFactory.get(fileAttribute);
        logger.info("预览文件url：{}，previewType：{}", fileUrl, fileAttribute.getType());
        //不进行encode,因为oss的链接有签名信息，特殊处理可能导致链接失效
//        fileUrl = WebUtils.urlEncoderencode(fileUrl);
        if (ObjectUtils.isEmpty(fileUrl)) {
            return otherFilePreview.notSupportedFile(model, "非法路径,不允许访问");
        }
        return filePreview.filePreviewHandle(fileUrl, model, fileAttribute);  //统一在这里处理 url
    }

    @GetMapping( "/picturesPreview")
    public String picturesPreview(String urls, Model model, HttpServletRequest req) {
        String fileUrls;
        try {
            fileUrls = WebUtils.decodeUrl(urls);
            // 防止XSS攻击
            fileUrls = KkFileUtils.htmlEscape(fileUrls);
            String fullFileName = WebUtils.getUrlParameterReg(fileUrls, "fullfilename", "filename");
            if (StringUtils.hasText(fullFileName)) {
                fileUrls = WebUtils.clearFullfilenameParam(fileUrls);
            }
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "urls");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
        logger.info("预览文件url：{}，urls：{}", fileUrls, urls);
        // 抽取文件并返回文件列表
        String[] images = fileUrls.split("\\|");
        List<String> imgUrls = Arrays.asList(images);
        model.addAttribute("imgUrls", imgUrls);
        String currentUrl = req.getParameter("currentUrl");
        if (StringUtils.hasText(currentUrl)) {
            String decodedCurrentUrl = new String(Base64.decodeBase64(currentUrl));
                   decodedCurrentUrl = KkFileUtils.htmlEscape(decodedCurrentUrl);   // 防止XSS攻击
            model.addAttribute("currentUrl", decodedCurrentUrl);
        } else {
            model.addAttribute("currentUrl", imgUrls.get(0));
        }
        return PICTURE_FILE_PREVIEW_PAGE;
    }

    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath  url
     * @param response response
     */
    @GetMapping("/getCorsFile")
    public void getCorsFile(String urlPath, HttpServletResponse response,FileAttribute fileAttribute) throws IOException {
        URL url;
        try {
            urlPath = WebUtils.decodeUrl(urlPath);
            String fullFileName = WebUtils.getUrlParameterReg(urlPath, "fullfilename", "filename");
            if (StringUtils.hasText(fullFileName)) {
                urlPath = WebUtils.clearFullfilenameParam(urlPath);
            }
            url = WebUtils.normalizedURL(urlPath);
        } catch (Exception ex) {
            logger.error(String.format(BASE64_DECODE_ERROR_MSG, urlPath),ex);
            return;
        }
        assert urlPath != null;
        if (!urlPath.toLowerCase().startsWith("http") && !urlPath.toLowerCase().startsWith("https") && !urlPath.toLowerCase().startsWith("ftp")) {
            logger.info("读取跨域文件异常，可能存在非法访问，urlPath：{}", urlPath);
            return;
        }
        InputStream inputStream = null;
        logger.info("读取跨域pdf文件url：{}", urlPath);
        if (!urlPath.toLowerCase().startsWith("ftp:")) {
            factory.setConnectionRequestTimeout(2000);
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(72000);
            HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();
            factory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(factory);
            RequestCallback requestCallback = request -> {
                request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
                String proxyAuthorization = fileAttribute.getKkProxyAuthorization();
                if(StringUtils.hasText(proxyAuthorization)){
                    Map<String,String> proxyAuthorizationMap = mapper.readValue(proxyAuthorization, Map.class);
                    proxyAuthorizationMap.forEach((key, value) -> request.getHeaders().set(key, value));
                }
            };
            try {
                restTemplate.execute(url.toURI(), HttpMethod.GET, requestCallback, fileResponse -> {
                    if(isPdf(url)){
//                        byte[] safeByte = PdfSafeUtil.sanitizePdfContent(IOUtils.toByteArray(fileResponse.getBody()));
//                        IOUtils.write(safeByte, response.getOutputStream());
                        // 严格的内容安全策略
                        // PDF预览优化的安全头
                        response.setHeader("Content-Security-Policy",
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-eval'; " +  // PDF.js需要eval
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self'");

                        response.setHeader("X-Content-Type-Options", "nosniff");
                        response.setHeader("X-Frame-Options", "SAMEORIGIN");  // 允许iframe预览

                        // 直接返回原始PDF内容，通过安全策略防护
                        response.setContentType("application/pdf");
                    }
//                    else{
                        IOUtils.copy(fileResponse.getBody(), response.getOutputStream());
//                    }
                    return null;
                });
            }  catch (Exception e) {
                logger.error("读取跨域文件异常，url：{}", urlPath,e);
            }
        }else{
            try {
                if(urlPath.contains(".svg")) {
                    response.setContentType("image/svg+xml");
                }
                inputStream = (url).openStream();
                IOUtils.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                logger.error("读取跨域文件异常，url：{}", urlPath);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    @GetMapping("/excelHiddenMeta")
    @ResponseBody
    public ReturnResponse<Object> excelHiddenMeta(String urlPath, FileAttribute fileAttribute) {
        URL url;
        try {
            urlPath = WebUtils.decodeUrl(urlPath);
            String fullFileName = WebUtils.getUrlParameterReg(urlPath, "fullfilename", "filename");
            if (StringUtils.hasText(fullFileName)) {
                urlPath = WebUtils.clearFullfilenameParam(urlPath);
            }
            url = WebUtils.normalizedURL(urlPath);
        } catch (Exception ex) {
            logger.error(String.format(BASE64_DECODE_ERROR_MSG, urlPath), ex);
            return ReturnResponse.failure("url参数解析失败");
        }
        if (urlPath == null) {
            return ReturnResponse.failure("url为空");
        }
        if (!urlPath.toLowerCase().startsWith("http") && !urlPath.toLowerCase().startsWith("https") && !urlPath.toLowerCase().startsWith("ftp")) {
            logger.info("读取跨域文件异常，可能存在非法访问，urlPath：{}", urlPath);
            return ReturnResponse.failure("非法访问");
        }
        byte[] bytes = null;
        if (!urlPath.toLowerCase().startsWith("ftp:")) {
            factory.setConnectionRequestTimeout(2000);
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(72000);
            HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();
            factory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(factory);
            RequestCallback requestCallback = request -> {
                request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
                String proxyAuthorization = fileAttribute.getKkProxyAuthorization();
                if (StringUtils.hasText(proxyAuthorization)) {
                    Map<String, String> proxyAuthorizationMap = mapper.readValue(proxyAuthorization, Map.class);
                    proxyAuthorizationMap.forEach((key, value) -> request.getHeaders().set(key, value));
                }
            };
            try {
                bytes = restTemplate.execute(url.toURI(), HttpMethod.GET, requestCallback, fileResponse -> IOUtils.toByteArray(fileResponse.getBody()));
            } catch (Exception e) {
                logger.error("读取文件异常，url：{}", urlPath, e);
                return ReturnResponse.failure("读取文件失败");
            }
        } else {
            try (InputStream inputStream = (url).openStream()) {
                bytes = IOUtils.toByteArray(inputStream);
            } catch (IOException e) {
                logger.error("读取ftp文件异常，url：{}", urlPath, e);
                return ReturnResponse.failure("读取文件失败");
            }
        }

        if (bytes == null || bytes.length == 0) {
            return ReturnResponse.failure("文件内容为空");
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> sheets = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i += 1) {
                Sheet sheet = workbook.getSheetAt(i);
                Set<Integer> hiddenCols = new HashSet<>();
                Set<Integer> hiddenRows = new HashSet<>();
                if (sheet instanceof XSSFSheet) {
                    XSSFSheet xssfSheet = (XSSFSheet) sheet;
                    List<CTCols> colsList = xssfSheet.getCTWorksheet().getColsList();
                    for (CTCols cols : colsList) {
                        for (CTCol col : cols.getColList()) {
                            if (col.isSetHidden() && col.getHidden()) {
                                long min = col.getMin();
                                long max = col.getMax();
                                for (long c = min; c <= max; c += 1) {
                                    hiddenCols.add((int) c - 1);
                                }
                            }
                        }
                    }
                    List<org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow> rowList = xssfSheet.getCTWorksheet().getSheetData().getRowList();
                    for (org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow row : rowList) {
                        if (row.isSetHidden() && row.getHidden()) {
                            hiddenRows.add((int) row.getR() - 1);
                        }
                    }
                } else {
                    int maxCol = 0;
                    int firstRow = sheet.getFirstRowNum();
                    int lastRow = sheet.getLastRowNum();
                    int limit = Math.min(lastRow, firstRow + 200);
                    for (int r = firstRow; r <= limit; r += 1) {
                        if (sheet.getRow(r) == null) continue;
                        short lastCellNum = sheet.getRow(r).getLastCellNum();
                        if (lastCellNum > maxCol) maxCol = lastCellNum;
                    }
                    for (int c = 0; c < maxCol; c += 1) {
                        if (sheet.isColumnHidden(c)) hiddenCols.add(c);
                    }
                    for (int r = firstRow; r <= lastRow; r += 1) {
                        if (sheet.getRow(r) == null) continue;
                        if (sheet.getRow(r).getZeroHeight()) hiddenRows.add(r);
                    }
                }
                Map<String, Object> sheetMeta = new HashMap<>();
                sheetMeta.put("index", i);
                sheetMeta.put("name", sheet.getSheetName());
                sheetMeta.put("hiddenCols", new ArrayList<>(hiddenCols));
                sheetMeta.put("hiddenRows", new ArrayList<>(hiddenRows));
                boolean hiddenFlag = workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i);
                String visibility = workbook.isSheetVeryHidden(i) ? "veryHidden" : (workbook.isSheetHidden(i) ? "hidden" : "visible");
                sheetMeta.put("hidden", hiddenFlag);
                sheetMeta.put("visibility", visibility);
                sheets.add(sheetMeta);
            }
        } catch (Exception e) {
            logger.error("解析excel隐藏列异常，url：{}", urlPath, e);
            return ReturnResponse.failure("解析excel失败");
        }
        result.put("sheets", sheets);
        return ReturnResponse.success(result);
    }

    private boolean isPdf(@NonNull URL url) {
        return url.getPath().contains("pdf") || url.getPath().contains("PDF");
    }

    /**
     * 通过api接口入队
     *
     * @param url 请编码后在入队
     */
    @GetMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        logger.info("添加转码队列url：{}", url);
        cacheService.addQueueTask(url);
        return "success";
    }

    @GetMapping( "/officeToPDF")
    @ResponseBody
    public ReturnResponse<Object> officeToPDF(String url, Model model, HttpServletRequest req) {
        String fileUrl;
        try {
//            fileUrl = WebUtils.decodeUrl(url);
            fileUrl = new String(Base64Utils.decodeFromString(url), "UTF-8");
            String fullFileName = WebUtils.getUrlParameterReg(fileUrl, "fullfilename", "filename");
            if (StringUtils.hasText(fullFileName)) {
                fileUrl = WebUtils.clearFullfilenameParam(fileUrl);
            }
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
            return ReturnResponse.failure(errorMsg);
        }
        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl,req);
        model.addAttribute("file", fileAttribute);
        FilePreview filePreview = previewFactory.get(fileAttribute);
        if(filePreview instanceof OfficeFilePreviewImpl){
            try{
                return ReturnResponse.success(((OfficeFilePreviewImpl) filePreview).officeToPDF(url, fileAttribute));
            }catch (Exception e){
                logger.error("转换pdf出错: {}", ExceptionUtils.getStackTrace(e));
                return ReturnResponse.failure(e.getMessage());
            }
        }
        return ReturnResponse.failure("只有office文件支持转pdf");
    }
}
