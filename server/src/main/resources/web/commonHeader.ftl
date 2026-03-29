<#setting classic_compatible=true>
<link rel="icon" href="./favicon.ico" type="image/x-icon">
<script src="js/watermark.js" type="text/javascript"></script>

<script>
    /**
     * 初始化水印
     */
    function initWaterMark() {

        <#if watermarkTxt?? >
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
      </#if>
    }

    /**
     * 从URL中提取参数
     */
    function getUrlParam(url, paramName) {
        var regex = new RegExp('[?&]' + paramName + '=([^&]+)');
        var match = regex.exec(url);
        return match ? decodeURIComponent(match[1]) : null;
    }

    /**
     * 添加返回按钮到压缩包预览
     */
    function addBackButtonToCompress() {
        var currentUrl = window.location.href;
        var urlParam = getUrlParam(currentUrl, 'url');

        if (!urlParam) {
            return;
        }

        var baseUrl = '${baseUrl}';
        var compressFileKey = null;
        var isFromCompress = false;

        try {
            var searchParams = new URLSearchParams(currentUrl.split('?')[1]);

            if (searchParams.has('url')) {
                try {
                    var url = searchParams.get('url');
                    var decodedUrl = atob(url);
                    compressFileKey = getUrlParam(decodedUrl, 'kkCompressfileKey');
                    if (compressFileKey) {
                        isFromCompress = true;
                    }
                } catch (e) {
                }
            }

            if (!compressFileKey && searchParams.has('kkCompressfileKey')) {
                compressFileKey = searchParams.get('kkCompressfileKey');
                if (compressFileKey) {
                    isFromCompress = true;
                }
            }
        } catch (e) {
            return;
        }

        if (!isFromCompress || !compressFileKey) {
            return;
        }

        var backBtn = document.createElement('button');
        backBtn.id = 'compressBackBtn';
        backBtn.innerHTML = '← 返回压缩包';
        backBtn.style.cssText = 'position: fixed; top: 10px; left: 10px; z-index: 10000; padding: 8px 16px; background-color: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; box-shadow: 0 2px 5px rgba(0,0,0,0.3); opacity: 0.5; transition: opacity 0.3s;';
        backBtn.onmouseover = function() { this.style.backgroundColor = '#45a049'; this.style.opacity = '1'; };
        backBtn.onmouseout = function() { this.style.backgroundColor = '#4CAF50'; this.style.opacity = '0.5'; };
        backBtn.onclick = function() {
            var backUrl = null;

            try {
                var referrer = document.referrer;
                if (referrer && referrer.indexOf(baseUrl) === 0) {
                    var referrerPath = referrer.substring(baseUrl.length);
                    if (referrerPath.startsWith('onlinePreview')) {
                        var referrerUrlParam = getUrlParam(referrer, 'url');
                        if (referrerUrlParam) {
                            var decodedReferrer = atob(referrerUrlParam);
                            var compressFileKeyInReferrer = getUrlParam(decodedReferrer, 'kkCompressfileKey');
                            var compressFilePathInReferrer = getUrlParam(decodedReferrer, 'kkCompressfilepath');

                            if (compressFileKeyInReferrer && !compressFilePathInReferrer) {
                                backUrl = referrer;

                                var watermarkTxtParam = getUrlParam(currentUrl, 'watermarkTxt');
                                if (watermarkTxtParam && getUrlParam(referrer, 'watermarkTxt') !== watermarkTxtParam) {
                                    if (referrer.indexOf('?') === -1) {
                                        backUrl += '?watermarkTxt=' + encodeURIComponent(watermarkTxtParam);
                                    } else {
                                        backUrl += '&watermarkTxt=' + encodeURIComponent(watermarkTxtParam);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e) {
            }

            if (backUrl) {
                window.location.href = backUrl;
            } else {
                window.history.back();
            }
        };
        document.body.appendChild(backBtn);
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

    #compressBackBtn {
        transition: background-color 0.3s;
    }

    #compressBackBtn:hover {
        background-color: #45a049 !important;
    }
</style>

<script>
    (function() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', addBackButtonToCompress);
        } else {
            addBackButtonToCompress();
        }
    })();
</script>
