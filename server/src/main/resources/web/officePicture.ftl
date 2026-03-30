<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <title>PDF Image Preview</title>
    <#include "*/commonHeader.ftl">
    <script src="js/lazyload.js"></script>
    <style>
        body {
               background-color: #404040;
           }
           .container {
               width: 100%;
               height: 100%;
           }
           .img-area {
               text-align: center;
           }
           .my-photo {
               max-width: 98%;
               margin:0 auto;
               border-radius:3px;
               box-shadow:rgba(0,0,0,0.15) 0 0 8px;
               background:#FBFBFB;
               border:1px solid #ddd;
               margin:1px auto;
               padding:5px;
           }
           .image-container {
               /*display: flex; !* 使用flexbox布局 *!*/
               /*justify-content: space-around; !* 图片间隔对齐 *!*/
                position: relative;
               display: inline-block;
           }
   
           .image-container img {
               width: 100%; /* 根据需要调整宽度 */
               height: auto; /* 保持图片的宽高比 */
           }
   
           .image-container img:hover {
               width: 150%; /* 根据需要调整宽度 */
               height: auto; /* 保持图片的宽高比 */
           }
   
           .button-container {
               position: absolute;
               top: 5px; /* 根据需要调整距离顶部的位置 */
               right: 10px; /* 根据需要调整距离右侧的位置 */
           }
   
           .button-container button {
               box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
               padding: 5px 10px 5px 22px;
               cursor: pointer;
               border: #b0b0b0 1px;
           }
           /*顺时针旋转90度*/
           .imgT90 {
               transform: rotate(90deg);
               -ms-transform: rotate(90deg);
               -webkit-transform: rotate(90deg);
               -o-transform: rotate(90deg);
               -moz-transform: rotate(90deg);
           }
   
           .imgT-90 {
               transform: rotate(-90deg);
               -ms-transform: rotate(-90deg);
               -webkit-transform: rotate(-90deg);
               -o-transform: rotate(-90deg);
               -moz-transform: rotate(-90deg);
           }
   
           /*顺时针旋转180度*/
           .imgT180 {
               transform: rotate(180deg);
               -ms-transform: rotate(180deg);
               -webkit-transform: rotate(180deg);
               -o-transform: rotate(180deg);
               -moz-transform: rotate(180deg);
           }
   
           .imgT-180 {
               transform: rotate(-180deg);
               -ms-transform: rotate(-180deg);
               -webkit-transform: rotate(-180deg);
               -o-transform: rotate(-180deg);
               -moz-transform: rotate(-180deg);
           }
   
           /*顺时针旋转270度*/
           .imgT270 {
               transform: rotate(270deg);
               -ms-transform: rotate(270deg);
               -webkit-transform: rotate(270deg);
               -o-transform: rotate(270deg);
               -moz-transform: rotate(270deg);
           }
   
           .imgT-270 {
               transform: rotate(-270deg);
               -ms-transform: rotate(-270deg);
               -webkit-transform: rotate(-270deg);
               -o-transform: rotate(-270deg);
               -moz-transform: rotate(-270deg);
           }
   
           .imgS150 {
               transform: scale(1.5);
               -ms-transform: scale(1.5);
               -webkit-transform: scale(1.5);
               -o-transform: scale(1.5);
               -moz-transform: scale(1.5);
           }
   
           .imgS200 {
               transform: scale(2);
               -ms-transform: scale(2);
               -webkit-transform: scale(2);
               -o-transform: scale(2);
               -moz-transform: scale(2);
           }

    </style>
</head>
<body>
<div class="container">
    <#list imgUrls as img>
        <div class="img-area" id="imgArea${img?index + 1}">
            <div class="image-container">
                <img class="my-photo" id="page${img?index + 1}" alt="loading" data-src="${img}" src="images/loading.gif">
                <div class="button-container">
                    <button>${img?index + 1}/${imgUrls?size}</button>
                    <button onclick="rotateImg('page${img?index + 1}', false)">Rotate CCW</button>
                    <button onclick="rotateImg('page${img?index + 1}', true)">Rotate CW</button>
                    <button onclick="recoveryImg('page${img?index + 1}')">Reset</button>
                </div>
            </div>
        </div>
    </#list>
</div>

<script>
    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
        checkImgs();
    };
    window.onscroll = throttle(checkImgs);

    function rotateImg(imgId, isRotate) {
        var img = document.querySelector("#" + imgId);
        if (img.classList.contains("imgT90")) {
            img.classList.remove("imgT90");
            if (isRotate) {
                img.classList.add("imgT180");
            }
        } else if (img.classList.contains("imgT-90")) {
            img.classList.remove("imgT-90");
            if (!isRotate) {
                img.classList.add("imgT-180");
            }
        } else if (img.classList.contains("imgT180")) {
            img.classList.remove("imgT180");
            if (isRotate) {
                img.classList.add("imgT270");
            } else {
                img.classList.add("imgT90");
            }
        } else if (img.classList.contains("imgT-180")) {
            img.classList.remove("imgT-180");
            if (isRotate) {
                img.classList.add("imgT-90");
            } else {
                img.classList.add("imgT-270");
            }
        } else if (img.classList.contains("imgT270")) {
            img.classList.remove("imgT270");
            if (!isRotate) {
                img.classList.add("imgT180");
            }
        } else if (img.classList.contains("imgT-270")) {
            img.classList.remove("imgT-270");
            if (isRotate) {
                img.classList.add("imgT-180");
            }
        } else {
            if (isRotate) {
                img.classList.add("imgT90");
            } else {
                img.classList.add("imgT-90");
            }
        }
    }

    function recoveryImg(imgId) {
        var img = document.querySelector("#" + imgId);
        img.classList.remove("imgT90", "imgT180", "imgT270", "imgT-90", "imgT-180", "imgT-270");
    }

    function changePreviewType(previewType) {
        var url = window.location.href;
        if (url.indexOf("officePreviewType=image") !== -1) {
            url = url.replace("officePreviewType=image", "officePreviewType="+previewType);
        } else {
            url = url + "&officePreviewType="+previewType;
        }
        if ('allImages' === previewType) {
            window.open(url)
        } else {
            window.location.href = url;
        }
    }
</script>
</body>
</html>
