<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>${file.name}预览</title>
    <#include "*/commonHeader.ftl">
    <link rel="stylesheet" href="xspreadsheet/xspreadsheet.css"/>
    <script src="xspreadsheet/xspreadsheet.js"></script>
    <script src="xspreadsheet/is-utf8.js"></script>
    <script src="xspreadsheet/xlsx.full.min.js"></script>
    <script src="xspreadsheet/xlsxspread.min.js"></script>
    <script src="xspreadsheet/zh-cn.js"></script>
    <script src="js/base64.min.js" type="text/javascript"></script>
    <style>
        /* 隐藏底部sheet 添加 */
        .x-spreadsheet-icon {
            display: none !important;
        }
        .x-spreadsheet-contextmenu {
            display: none !important;
        }
        /* 禁用底部 sheet 的双击及选中 */
        .x-spreadsheet-bottombar {
            pointer-events: none;
        }
        .x-spreadsheet-bottombar * {
            pointer-events: none;
        }
    </style>
</head>
<#if csvUrl?contains("http://") || csvUrl?contains("https://")>
    <#assign finalUrl="${csvUrl}">
<#elseif csvUrl?contains("ftp://") >
    <#assign finalUrl="${csvUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${csvUrl}">
</#if>
<body>
<div id="htmlout"></div>
<script>
    x_spreadsheet.locale('en');
    var HTMLOUT = document.getElementById('htmlout');
    console.log(HTMLOUT);
    const options = {
        mode: 'read',
        showToolbar: false,// 是否显示工具栏
        showGrid: true,// 是否显示网格线
        showContextmenu: false,// 是否显示右键菜单

    };
    //加载表格
    var xspr = x_spreadsheet(HTMLOUT,options);
    (function () {
        function inBottomBar(target) {
            var el = target;
            while (el && el !== HTMLOUT && el.nodeType === 1) {
                if (el.classList && el.classList.contains('x-spreadsheet-bottombar')) return true;
                el = el.parentNode;
            }
            return false;
        }
        HTMLOUT.addEventListener('contextmenu', function (e) {
            if (inBottomBar(e.target)) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        }, true);
        HTMLOUT.addEventListener('mousedown', function (e) {
            if (e.button === 2 && inBottomBar(e.target)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);
        HTMLOUT.addEventListener('dblclick', function (e) {
            if (inBottomBar(e.target)) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        }, true);
    })();
    // HTMLOUT.style.height = (window.innerHeight - 400) + "px";
    // HTMLOUT.style.width = (window.innerWidth - 50) + "px";

    var process_wb = (function () {
        return function process_wb(wb) {
            var data = stox(wb);
            xspr.loadData(data);
            if (typeof console !== 'undefined') console.log("output", new Date());
        };
    })();
    var url = '${finalUrl}';
    console.log(url);
    var baseUrl = window.location.origin ? window.location.origin + '/kkview/' : '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';;
    if (!url.startsWith(baseUrl)) {
        url = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(url));
    }
    let xhr = new XMLHttpRequest();
    xhr.open('GET', url); //文件所在地址
    xhr.responseType = 'blob';
    xhr.onload = () => {
        let content = xhr.response;
        let blob = new Blob([content]);
        let file = new File([blob], 'excel.csv', {type: 'excel/csv'});
        var reader = new FileReader();
        reader.onload = function (e) {
            if (typeof console !== 'undefined') console.log("onload", new Date());
            var data = e.target.result;
            data = new Uint8Array(data);
            let decoded = false;

          // 检测并处理UTF-8 BOM
          let hasBOM = false;
          if (data.length >= 3 && data[0] === 0xEF && data[1] === 0xBB && data[2] === 0xBF) {
              hasBOM = true;
              // 移除BOM
              data = data.subarray(3);
              if(typeof console !== 'undefined') console.log("Removed UTF-8 BOM");
          }

          // 专业的编码检测函数
          function detectEncoding(bytes) {
              // 1. 检测UTF-8
              if (isUTF8(bytes)) {
                  return 'utf-8';
              }

              // 2. 检测Shift-JIS (Windows-31J)
              // Shift-JIS特征：
              // - 0x81-0x9F, 0xE0-0xFC 作为前导字节
              // - 后跟 0x40-0x7E, 0x80-0xFC
              let hasShiftJIS = false;
              let shiftJISCount = 0;
              for (let i = 0; i < bytes.length; i++) {
                  let b = bytes[i];
                  if ((b >= 0x81 && b <= 0x9F) || (b >= 0xE0 && b <= 0xFC)) {
                      // 前导字节
                      if (i + 1 < bytes.length) {
                          let next = bytes[i + 1];
                          if ((next >= 0x40 && next <= 0x7E) || (next >= 0x80 && next <= 0xFC)) {
                              shiftJISCount++;
                              i++;
                          }
                      }
                  }
              }
              // 如果Shift-JIS特征字节超过总字节的10%，认为是Shift-JIS
              if (shiftJISCount > bytes.length * 0.1) {
                  return 'shift-jis';
              }

              // 3. 检测GBK/GB2312
              // GBK特征：
              // - 0x81-0xFE 作为前导字节
              // - 后跟 0x40-0xFE (除0x7F)
              let hasGBK = false;
              let gbkCount = 0;
              for (let i = 0; i < bytes.length; i++) {
                  let b = bytes[i];
                  if (b >= 0x81 && b <= 0xFE) {
                      // 前导字节
                      if (i + 1 < bytes.length) {
                          let next = bytes[i + 1];
                          if ((next >= 0x40 && next <= 0xFE) && next !== 0x7F) {
                              gbkCount++;
                              i++;
                          }
                      }
                  }
              }
              // 如果GBK特征字节超过总字节的10%，认为是GBK
              if (gbkCount > bytes.length * 0.1) {
                  return 'gbk';
              }

              // 4. 默认为UTF-8
              return 'utf-8';
          }

          // 检测文件编码
          let detectedEncoding = detectEncoding(data);
          if(typeof console !== 'undefined') console.log("Detected encoding:", detectedEncoding);

          // 尝试使用检测到的编码
          if (typeof TextDecoder !== 'undefined') {
              try {
                  var str = new TextDecoder(detectedEncoding).decode(data);
                  // 验证解码结果
                  if (str && str.length > 0) {
                      // 输出前100个字符作为样本
                      var sample = str.substring(0, Math.min(100, str.length));
                      if(typeof console !== 'undefined') console.log("Decoded with detected encoding", detectedEncoding, ":", sample);
                      process_wb(XLSX.read(str, { type: "string" }));
                      decoded = true;
                      if(typeof console !== 'undefined') console.log("=== SUCCESS: Successfully decoded with detected encoding:", detectedEncoding, "===");
                  }
              } catch (error) {
                  if(typeof console !== 'undefined') console.log("Failed to decode with detected encoding", detectedEncoding, ":", error);
              }
          }

          // 如果检测的编码失败，尝试其他编码
          if (!decoded) {
              if(typeof console !== 'undefined') console.log("Trying fallback encodings...");

              // 按优先级尝试其他编码
              let fallbackEncodings = ['utf-8', 'shift-jis', 'gbk', 'big5', 'windows-1252', 'euc-jp'];
              // 移除已经尝试过的编码
              fallbackEncodings = fallbackEncodings.filter(enc => enc !== detectedEncoding);

              if (typeof TextDecoder !== 'undefined') {
                  for (let encoding of fallbackEncodings) {
                      try {
                          var str = new TextDecoder(encoding).decode(data);
                          if (str && str.length > 0) {
                              var sample = str.substring(0, Math.min(100, str.length));
                              if(typeof console !== 'undefined') console.log("Fallback decoding with", encoding, ":", sample);
                              process_wb(XLSX.read(str, { type: "string" }));
                              decoded = true;
                              if(typeof console !== 'undefined') console.log("=== SUCCESS: Successfully decoded with fallback encoding:", encoding, "===");
                              break;
                          }
                      } catch (error) {
                          if(typeof console !== 'undefined') console.log("Fallback failed with", encoding, ":", error);
                      }
                  }
              }

              // 如果TextDecoder失败，尝试使用cptable
              if (!decoded && typeof cptable !== 'undefined' && typeof cptable.utils !== 'undefined' && typeof cptable.utils.decode !== 'undefined') {
                  let cptableEncodings = [
                      { code: 65001, name: 'UTF-8' },
                      { code: 932, name: 'SHIFT_JIS' },
                      { code: 936, name: 'GB2312' },
                      { code: 950, name: 'BIG5' },
                      { code: 1252, name: 'Windows-1252' },
                      { code: 20932, name: 'GBK' },
                      { code: 51932, name: 'EUC-JP' }
                  ];

                  for (let encoding of cptableEncodings) {
                      try {
                          var str = cptable.utils.decode(encoding.code, data);
                          if (str && str.length > 0) {
                              var sample = str.substring(0, Math.min(100, str.length));
                              if(typeof console !== 'undefined') console.log("cptable decoding with", encoding.name, ":", sample);
                              process_wb(XLSX.read(str, { type: "string" }));
                              decoded = true;
                              if(typeof console !== 'undefined') console.log("=== SUCCESS: Successfully decoded with cptable:", encoding.name, "===");
                              break;
                          }
                      } catch (error) {
                          if(typeof console !== 'undefined') console.log("cptable failed with", encoding.name, ":", error);
                      }
                  }
              }
          }

          // 如果所有编码尝试都失败，显示错误信息
          if (!decoded) {
              if(typeof console !== 'undefined') console.log("All encoding attempts failed");
              // 显示错误信息
              HTMLOUT.innerHTML = '<div style="padding: 20px; color: red;">文件编码解析失败，无法预览</div>';
          }
        };
        reader.readAsArrayBuffer(file);
    }
    xhr.send();
    /*初始化水印*/
    if (!!window.ActiveXObject || "ActiveXObject" in window) {
    } else {
        initWaterMark();
    }
</script>

</body>
</html>
