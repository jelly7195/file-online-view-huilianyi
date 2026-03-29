<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <title>图片预览</title>
    <#include "*/commonHeader.ftl">
    <link rel="stylesheet" href="css/viewer.min.css">
    <script src="js/viewer.min.js"></script>
    <script src="js/base64.min.js" type="text/javascript"></script>
    <style>
        body {
            background-color: #404040;
        }
        #image { width: 800px; margin: 0 auto; font-size: 0;}
        #image li {  display: inline-block;width: 50px;height: 50px; margin-left: 1%; padding-top: 1%;}
        /*#dowebok li img { width: 200%;}*/
    </style>
</head>
<body>

<ul id="image">
    <#list imgUrls as img>
        <#if img?contains("http://") || img?contains("https://")>
            <#assign img="${img}">
        <#else>
            <#assign img="${baseUrl}${img}">
        </#if>
        <li><img id="img-${img_index}" src="" style="display: none" alt="image"></li>
        <script>
            var pdfUrl = '${img}';
            // console.log("原始图片URL: " + pdfUrl);
            <#--console.log("图片index: " + `${img_index}`);-->


            var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
            if (!pdfUrl.startsWith(baseUrl)) {
                // console.log("跨域: " + pdfUrl);
                pdfUrl = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(pdfUrl));
                // console.log("跨域处理后: " + pdfUrl);
            }

            // 将处理后的URL赋值给当前img标签的src属性
            document.getElementById("img-${img_index}").src = pdfUrl;
        </script>
    </#list>
</ul>

<script>
    var viewer = new Viewer(document.getElementById('image'), {
        url: 'src',//指定图片的 URL 来源是 <img> 标签的 src 属性
        title: false,//禁用标题栏
        navbar: true,//禁用导航栏
        button: false,//禁用工具栏按钮（如放大、缩小、旋转等）
        toolbar: false,//禁用工具栏
        backdrop: false,
        loop : false //禁用图片循环浏览功能
    });
    // document.getElementById("img").click();

    // 如果有图片，点击第一张图片
    var firstImg = document.querySelector("#image img");
    if (firstImg) {
        console.log("firstImg")
        firstImg.click();
    }

    /*初始化水印*/
    window.onload = function() {
        initWaterMark();
    }
</script>
</body>

</html>
