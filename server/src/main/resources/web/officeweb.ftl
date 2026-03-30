<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>${file.name}预览</title>
    <link rel='stylesheet' href='xlsx/plugins/css/pluginsCss.css' />
    <link rel='stylesheet' href='xlsx/plugins/plugins.css' />
    <link rel='stylesheet' href='xlsx/css/luckysheet.css' />
    <link rel='stylesheet' href='xlsx/assets/iconfont/iconfont.css' />
    <script src="xlsx/plugins/js/plugin.js"></script>
    <script src="xlsx/luckysheet.umd.js"></script>
    <script src="js/watermark.js" type="text/javascript"></script>
    <script src="js/base64.min.js" type="text/javascript"></script>
</head>
<#if pdfUrl?contains("http://") || pdfUrl?contains("https://") || pdfUrl?contains("ftp://")>
    <#assign finalUrl="${pdfUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${pdfUrl}">
</#if>
<script>
    /**
     * 初始化水印
     */
    function initWaterMark() {

        let watermarkTxt = '${watermarkTxt}';
        if (watermarkTxt !== '') {
            watermark.init({
                watermark_txt: '${watermarkTxt}',
                watermark_x: 0,
                watermark_y: 0,
                watermark_rows: 0,
                watermark_cols: 0,
                watermark_x_space: ${watermarkXSpace},
                watermark_y_space: ${watermarkYSpace},
                watermark_font: '${watermarkFont}',
                watermark_fontsize: '${watermarkFontsize}',
                watermark_color: '${watermarkColor}',
                watermark_alpha: ${watermarkAlpha},
                watermark_width: ${watermarkWidth},
                watermark_height: ${watermarkHeight},
                watermark_angle: ${watermarkAngle},
            });
        }
    }

</script>
<style>
    * {
        margin: 0;
        padding: 0;
    }

    html, body {
        height: 100%;
        width: 100%;
    }

</style>
<body>
<div id="lucky-mask-demo" style="position: absolute;z-index: 1000000;left: 0px;top: 0px;bottom: 0px;right: 0px; background: rgba(255, 255, 255, 0.8); text-align: center;font-size: 40px;align-items:center;justify-content: center;display: none;">加载中</div>
<p style="text-align:center;">
<#-- 切换html 预览按钮，隐藏 -->
<#--<div id="button-area">-->
<#--    <label><button onclick="tiaozhuan()">跳转HTML预览</button></label>-->
<#--    <button id="confirm-button" onclick="print()">打印</button>-->
<#--</div>-->
<div id="luckysheet" style="margin:0px;padding:0px;position:absolute;width:100%;left: 0px;top: 20px;bottom: 0px;outline: none;"></div>

<script src="xlsx/luckyexcel.umd.js"></script>
<script>
    function tiaozhuan(){
        var test = window.location.href;
        test = test.replace(new RegExp("&officePreviewType=xlsx",("gm")),"");
        test = test+'&officePreviewType=html';
        window.location.href=test;
    }
    var url = '${finalUrl}';
    var baseUrl = window.location.origin ? window.location.origin + '/kkview/' : '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';;
    if (!url.startsWith(baseUrl)) {
        url = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(url));
    }
    let mask = document.getElementById("lucky-mask-demo");
    function loadText() {
        initWaterMark(); // 是否显示水印
        var value = url;
        var name = '${file.name}';
        if(value==""){
            return;
        }
        // mask.style.display = "flex";
         LuckyExcel.transformExcelToLuckyByUrl(value, name, function(exportJson, luckysheetfile){
            if(exportJson.sheets==null || exportJson.sheets.length==0){
                alert("读取excel文件内容失败!");
                return;
            }
            mask.style.display = "none";
            window.luckysheet.destroy();
            window.luckysheet.create({
                container: 'luckysheet', //luckysheet is the container id
                lang: "en",
                showtoolbarConfig:{
                    image: false,//插入图片
                    print: false, //关闭打印按钮  启用也不能用 等以后看情况而定
                    exportXlsx: false, //关闭导出按钮  启用也不能用  等以后看情况而定
                },
                allowCopy: true, // 是否允许拷贝
                showtoolbar: false, // 是否显示工具栏
                showinfobar: false, // 是否显示顶部信息栏
                // myFolderUrl: "/",//作用：左上角<返回按钮的链接
                showsheetbar: false, // 是否显示底部sheet页按钮
                showsheetbarConfig:{
                    // add: true, // 是否显示添加按钮
                    menu: true, // 是否显示菜单按钮
                    sheet: true, // 是否显示sheet页名称
                },
                sheetRightClickConfig:{
                 delete: false, // 删除
                 copy: false, // 复制
                 rename: false, //重命名
                 color: false, //更改颜色
                 hide: false, //隐藏，取消隐藏
                 move: false, //向左移，向右移
                },
                showstatisticBar: true, // 是否显示底部计数栏
                sheetBottomConfig: true, // sheet页下方的添加行按钮和回到顶部按钮配置
                allowEdit: false, // 是否允许前台编辑
                enableAddRow: false, // 允许增加行
                enableAddCol: false, // 允许增加列
                userInfo: false, // 右上角的用户信息展示样式
                showRowBar: true, // 是否显示行号区域
                showColumnBar: false, // 是否显示列号区域
                sheetFormulaBar: false, // 是否显示公式栏
                enableAddBackTop: true,//返回头部按钮
                forceCalculation: false, //下面是导出插件 默认关闭
                        data: exportJson.sheets,
                        title: exportJson.info.name,
                        userInfo: exportJson.info.name.creator,
            });
        }, 500);
    }
    loadText();
    // 打印时，获取luckysheet指定区域html内容，拼接至div，隐藏luckysheet容器并显示打印区域html
    function to_print() {
        const html = luckysheet.getRangeHtml();
        document.querySelector('#print-html').innerHTML = html;
        document.querySelector('#print-area').style.display = 'block';
        document.querySelector('#button-area').style.display = 'none';
    }
</script>
</body>
</html>