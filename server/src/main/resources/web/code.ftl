<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>代码预览</title>
    <#include  "*/commonHeader.ftl">
    <script src="js/jquery-3.6.1.min.js" type="text/javascript"></script>
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <script src="bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
    <link rel="stylesheet" href="highlight/default.min.css">
    <script src="highlight/highlight.min.js" type="text/javascript"></script>
    <script src="js/base64.min.js" type="text/javascript"></script>
<#--    <script>hljs.highlightAll()</script>-->

    <style>
        /*div.code {*/
        /*    white-space: pre;*/
        /*}*/
        .code-container {
            background-color: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 0.375rem;
            padding: 1rem;
            margin-bottom: 1rem;
            max-height: 92vh;
            overflow: auto;
            font-family: 'SFMono-Regular', 'Consolas', 'Liberation Mono', 'Menlo', monospace;
            font-size: 0.875rem;
            line-height: 1.45;
        }

        .code-container pre {
            margin: 0;
            background: transparent;
            border: none;
            padding: 0;
        }

        .code-container code {
            background: transparent;
            padding: 0;
            color: inherit;
        }

        /* 响应式调整 */
        @media (max-width: 768px) {
            .code-container {
                font-size: 0.8rem;
                padding: 0.75rem;
            }
        }

        /* 滚动条样式优化 */
        .code-container::-webkit-scrollbar {
            width: 8px;
        }

        .code-container::-webkit-scrollbar-track {
            background: #f1f1f1;
            border-radius: 4px;
        }

        .code-container::-webkit-scrollbar-thumb {
            background: #c1c1c1;
            border-radius: 4px;
        }

        .code-container::-webkit-scrollbar-thumb:hover {
            background: #a8a8a8;
        }
    </style>
</head>
<body>

<input hidden id="textData" value="${textData}"/>

<div class="container">
    <div class="card">
        <div class="card-header bg-light d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0">
                <a data-toggle="collapse" href="#codeCollapse" class="text-decoration-none text-dark">
                    ${file.name}
                </a>
            </h5>
            <small class="text-muted">代码预览</small>
        </div>
        <div class="collapse show" id="codeCollapse">
            <div class="card-body p-0">
                <div id="code" class="code-container"></div>
            </div>
        </div>
    </div>
</div>


<script>
    /**
     * 初始化
     */
    window.onload = function () {
        initWaterMark();
        loadText();
    }

    /**
     * 加载代码文本
     */
    function loadText() {
        var base64data = $("#textData").val()
        var textData = Base64.decode(base64data);

        // 检测文件扩展名以确定语言
        var fileName = "${file.name}";
        var fileExt = fileName.split('.').pop().toLowerCase();
        var languageClass = detectLanguage(fileExt);

        var textPreData = "<pre><code class='" + languageClass + "'>" + escapeHtml(textData) + "</code></pre>";
        $("#code").append(textPreData);

        // 在内容加载后执行高亮
        setTimeout(function() {
            hljs.highlightAll();
        }, 100);
    }

    /**
     * 根据文件扩展名检测语言类型
     */
    function detectLanguage(ext) {
        var languageMap = {
            'js': 'javascript',
            'java': 'java',
            'py': 'python',
            'php': 'php',
            'html': 'html',
            'htm': 'html',
            'css': 'css',
            'xml': 'xml',
            'json': 'json',
            'sql': 'sql',
            'sh': 'bash',
            'bat': 'batch',
            'cpp': 'cpp',
            'c': 'c',
            'cs': 'csharp',
            'go': 'go',
            'rb': 'ruby',
            'swift': 'swift',
            'kt': 'kotlin',
            'ts': 'typescript',
            'vue': 'html',
            'md': 'markdown',
            'yaml': 'yaml',
            'yml': 'yaml',
            'properties': 'properties',
            'ini': 'ini',
            'conf': 'ini',
            'log': 'plaintext',
            'txt': 'plaintext'
        };

        return languageMap[ext] || 'plaintext';
    }

    /**
     * HTML转义函数
     */
    function escapeHtml(text) {
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };

        return text.replace(/[&<>"']/g, function(m) {
            return map[m];
        });
    }


</script>

</body>

</html>
