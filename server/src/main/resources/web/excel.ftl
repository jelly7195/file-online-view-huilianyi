<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>在HTML中使用vue-office-excel</title>

    <#include "*/commonHeader.ftl">
    <script src="js/base64.min.js" type="text/javascript"></script>

    <style>
        /* 确保页面占满全屏 */
        html, body {
            margin: 0;
            padding: 0;
            height: 100%;
            overflow: hidden; /* 避免页面整体滚动 */
        }

    </style>
</head>
<body>
<iframe src="" width="100%" frameborder="0"></iframe>
<script>

    var pdfUrl = '${pdfUrl}';

    var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
    if (!pdfUrl.startsWith(baseUrl)) {
        pdfUrl = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(pdfUrl));
    }

    document.getElementsByTagName('iframe')[0].src =  "${baseUrl}univer/index.html?file="+encodeURIComponent(pdfUrl);
    document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight - 10;

    /**
     * 页面变化调整高度
     */
    // window.onresize = function () {
    //     var fm = document.getElementsByTagName("iframe")[0];
    //     fm.height = window.document.documentElement.clientHeight - 10;
    // }
</script>
</body>
</html>
