
package cn.keking.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @Description: PDF 安全处理工具类，防止 XSS 注入
 * @ClassName: PdfSafeUtil
 * @Author: xin.zhou
 * @Date: 2025/7/23 15:48
 */
public class PdfSafeUtil {

    /**
     * 移除 PDF 中的 JavaScript 脚本和动作，防止 XSS 注入
     *
     * @param pdfContent 原始 PDF 字节流
     * @return 安全处理后的 PDF 字节流
     * @throws IOException 如果处理失败
     */
    public static byte[] sanitizePdfContent(byte[] pdfContent) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfContent)) {

            // 1. 移除文档级别的动作（如 OpenAction）
            COSDictionary root = document.getDocumentCatalog().getCOSObject();
            root.removeItem(COSName.OPEN_ACTION);
            root.removeItem(COSName.AA); // Additional Actions

            // 2. 移除所有页面的动作（如 Page Actions）
            for (PDPage page : document.getPages()) {
                COSDictionary pageDict = page.getCOSObject();
                pageDict.removeItem(COSName.AA); // Page Additional Actions
                // 不移除 ANNOTS，因为这会移除书签
//                pageDict.removeItem(COSName.ANNOTS); // 移除注释中的动作（可选）
            }

            // 3. 移除所有注释中的动作（可能包含 JavaScript）
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                for (PDAnnotation annotation : annotations) {
                    COSDictionary annotDict = annotation.getCOSObject();
                    if (annotDict.containsKey(COSName.A)) {
                        annotDict.removeItem(COSName.A); // 移除动作
                    }
                }
            }

            // 4. 移除大纲（书签）中的 JavaScript 动作
            document.getDocumentCatalog().setActions(null);

            removeDocumentLevelJavaScript(document);

            // 5. 移除表单字段中的 JavaScript
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                acroForm.getCOSObject().removeItem(COSName.AA); // 表单全局动作
            }

            // 保存处理后的 PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }


    /**
     * 移除文档级JavaScript（Catalog/Names/JavaScript）
     */
    private static void removeDocumentLevelJavaScript(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDDocumentNameDictionary names = catalog.getNames();
        if (names != null) {
            // 获取文档级JavaScript条目并移除
            PDJavascriptNameTreeNode javaScriptNames = names.getJavaScript();
            if (javaScriptNames != null) {
                names.setJavascript(null);  // 清除JavaScript条目
                System.out.println("已移除文档级JavaScript");
            }
        }
    }


}