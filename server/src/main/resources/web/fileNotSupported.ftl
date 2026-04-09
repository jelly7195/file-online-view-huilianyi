<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <style type="text/css">
        :root {
            --bg: #f6f7fb;
            --card: #ffffff;
            --text: #111827;
            --muted: #6b7280;
            --danger: #dc2626;
            --border: #e5e7eb;
        }

        * { box-sizing: border-box; }

        body {
            margin: 0;
            min-height: 100vh;
            background: radial-gradient(1200px 600px at 20% 0%, #eef2ff 0%, rgba(238,242,255,0) 50%),
                        radial-gradient(900px 500px at 90% 20%, #ecfeff 0%, rgba(236,254,255,0) 55%),
                        var(--bg);
            color: var(--text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans CJK SC", "Noto Sans SC", sans-serif;
        }

        .page {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px 16px;
        }

        .card {
            width: 100%;
            max-width: 720px;
            background: var(--card);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(17, 24, 39, 0.08);
        }

        .icon {
            width: 44px;
            height: 44px;
            border-radius: 999px;
            background: #fee2e2;
            color: var(--danger);
            display: grid;
            place-items: center;
            flex: 0 0 auto;
            font-weight: 700;
            font-size: 22px;
            line-height: 1;
        }

        .message {
            margin: 14px 0 0;
            font-size: 18px;
            line-height: 28px;
            font-weight: 700;
        }

    </style>
</head>

<body>
<div class="page">
    <div class="card">
        <div class="icon">!</div>
        <div class="message">Sorry, the preview timed out. Please download the file to view it.</div>
    </div>
</div>
</body>
</html>
