<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>xml文本预览</title>
    <#include  "*/commonHeader.ftl">
    <script src="js/jquery-3.6.1.min.js" type="text/javascript"></script>
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <script src="bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
    <link rel="stylesheet" href="css/xmlTreeViewer.css"/>
    <script src="js/xmlTreeViewer.js" type="text/javascript"></script>
    <script src="js/base64.min.js" type="text/javascript"></script>
</head>
<body>

<input hidden id="textData" value="${textData}"/>
<div class="container">
    <div class="card">
        <div id="xml_btn" class="card-header">
            <h5 class="card-title mb-0">
                <a data-toggle="collapse" href="#collapseOne">
                    ${file.name}
                </a>
            </h5>
        </div>
        <div id="text_btn" class="card-header">
            <h5 class="card-title mb-0">
                <a data-toggle="collapse" href="#collapseOne">
                    ${file.name}
                </a>
            </h5>
        </div>
        <div class="card-body">
            <div id="xml"></div>
        </div>
    </div>
</div>

<script>
    /**
     * 初始化
     */
    window.onload = function () {
        $("#xml_btn").hide()
        initWaterMark();
        loadXmlData()
    }

    /**
     * 加载xml数据
     */
    function htmlEscape(str){
        var s = "";
        if(str.length == 0) return "";
        s = str.replace(/&amp;/g,"&");
        s = str.replace(/&amp;amp;/g,"&");
        s = s.replace(/&lt;/g,"<");
        s = s.replace(/&gt;/g,">");
        s = s.replace(/&nbsp;/g," ");
        s = s.replace(/&#39;/g,"\'");
        s = s.replace(/&quot;/g,"\"");
        s = s.replace(/<script.*?>.*?<\/script>/ig, '');
        s = s.replace(/<script/gi, "&lt;script ");
        s = s.replace(/<iframe/gi, "&lt;iframe ");
        return s;
    }
    function loadXmlData() {
        var textData = Base64.decode($("#textData").val())
        textData = htmlEscape(textData);
        window.textPreData = "<xmp style='background-color: #FFFFFF;overflow-y: scroll;border:none'>" + textData + "</xmp>";
        var xmlNode = xmlTreeViewer.parseXML(textData);
        window.retNode = xmlTreeViewer.getXMLViewerNode(xmlNode.xml);
        $("#xml").html(window.retNode);
    }

    $(function () {
        $("#xml_btn").click(function () {
            $("#xml").html(window.retNode);
            $("#text_btn").show()
            $("#xml_btn").hide()
        });

        $("#text_btn").click(function () {
            $("#xml_btn").show()
            $("#text_btn").hide();
            $("#xml").html(window.textPreData);
        });
    });
</script>
</body>

</html>
