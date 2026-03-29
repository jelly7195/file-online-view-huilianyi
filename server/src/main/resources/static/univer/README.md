# Excel 在线预览 —— 实现文档

## 一、整体架构

本方案是一个**纯前端、零后端依赖**的 Excel 在线预览页面，所有解析和渲染逻辑在浏览器中完成。页面通过 URL 参数 `?url=文件地址` 接收要预览的 xlsx 文件，自动下载、解析并渲染为只读电子表格。

```
┌─────────────────────────────────────────────────────────────┐
│                        URL 入口层                            │
│       ?url=https://example.com/file.xlsx                    │
├─────────────────────────────────────────────────────────────┤
│                     文件下载层                                │
│       fetch(url) → ArrayBuffer → File 对象                  │
├─────────────────────────────────────────────────────────────┤
│                     文件解析与转换层                           │
│  ┌─────────────────────────┐   ┌──────────────────────────┐ │
│  │ 主解析器: LuckyExcel     │──→│ 备用解析器: ExcelJS       │ │
│  │ xlsx → Univer 一键转换   │失败│ xlsx → 手动转换 Univer   │ │
│  └─────────────────────────┘   └──────────────────────────┘ │
│                         │                                    │
│                         ↓                                    │
│              ┌──────────────────────┐                        │
│              │ ExcelJS 文本校正层    │                        │
│              │ 修正 LuckyExcel 的   │                        │
│              │ 单元格显示值偏差      │                        │
│              └──────────────────────┘                        │
├─────────────────────────────────────────────────────────────┤
│                       渲染引擎层                              │
│       UniverJS (Sheets Core + Drawing 预设)                  │
│       接收 IWorkbookData → 渲染为只读电子表格界面              │
├─────────────────────────────────────────────────────────────┤
│                       权限控制层                              │
│       Workbook 级别 + Worksheet 级别双重只读保护              │
└─────────────────────────────────────────────────────────────┘
```

### 完整数据流向

```
浏览器打开 ?url=xxx.xlsx
       ↓
fetch(url) 下载文件 → ArrayBuffer
       ↓
构造 File 对象（保留文件名和 MIME 类型）
       ↓
importFile(file) 进入解析流程
       │
       ├── LuckyExcel 可用？
       │       │
       │    是 ↓
       │    [主解析器] LuckyExcel.transformExcelToUniver(file, callback)
       │       │
       │       ├── 成功 ──→ 得到 IWorkbookData (univerData)
       │       │                ↓
       │       │          fixCellValues(univerData, file)
       │       │          用 ExcelJS 重新读取文件，逐单元格校正显示值
       │       │                ↓
       │       │          univerAPI.createWorkbook(univerData)
       │       │                ↓
       │       │          setWorkbookReadOnly(fWorkbook)
       │       │          双重只读保护（Workbook + 所有 Worksheet）
       │       │
       │       └── 失败/超时(15s) ──→ 自动回退到 ExcelJS
       │
       └── LuckyExcel 不可用
               ↓
        [备用解析器] parseWithExcelJS(file)
        ExcelJS 解析 + 手动转换为 Univer 数据格式
               ↓
        univerAPI.createWorkbook(workbookData)
               ↓
        setWorkbookReadOnly(fWorkbook)
```

---

## 二、CDN 依赖说明

共加载 **8 个 JS + 2 个 CSS**，分为四组：

| 序号 | 依赖 | 作用 | 全局变量 |
|------|------|------|----------|
| 1 | `react@18.3.1` | Univer 的 UI 渲染依赖 | `React` |
| 2 | `react-dom@18.3.1` | React DOM 渲染器 | `ReactDOM` |
| 3 | `rxjs` | Univer 的响应式数据流依赖 | `rxjs` |
| 4 | `@univerjs/presets` | Univer 预设工厂函数 | `UniverPresets`（提供 `createUniver`） |
| 5 | `@univerjs/preset-sheets-core` + locale + CSS | 电子表格核心功能 | `UniverPresetSheetsCore`, `UniverPresetSheetsCoreZhCN` |
| 6 | `@univerjs/preset-sheets-drawing` + locale + CSS | 图片/图形绘制支持 | `UniverPresetSheetsDrawing`, `UniverPresetSheetsDrawingZhCN` |
| 7 | `@mertdeveci55/univer-import-export@0.2.1` | **主解析器**：xlsx → Univer 一键转换 | `LuckyExcel` |
| 8 | `exceljs` | **备用解析器** + 文本校正引擎 | `ExcelJS` |

**加载顺序**：React/RxJS → Univer Presets → Univer Core/Drawing → LuckyExcel → ExcelJS。

---

## 三、页面布局

采用 **Flexbox 纵向布局**，页面只有预览区域和加载遮罩两个元素：

```html
<body>
    <div class="preview-container" id="app"></div>
    <div class="loading-overlay" id="loading">正在解析 Excel 文件，请稍候...</div>
</body>
```

| 选择器 | CSS 规则 | 作用 |
|--------|----------|------|
| `html, body` | `height: 100%; overflow: hidden; margin: 0` | 全屏无滚动条 |
| `body` | `display: flex; flex-direction: column` | 纵向弹性布局 |
| `.preview-container` | `flex: 1; min-height: 0; overflow: hidden` | 占满全部空间，`min-height: 0` 防止 flex 溢出 |
| `.loading-overlay` | `position: fixed; z-index: 9999` | 全屏半透明遮罩，解析时显示 |

Univer 初始化时自动挂载到 `#app` 容器，配置了 `toolbar: false, formulaBar: false, contextMenu: false` 隐藏所有编辑相关 UI，呈现纯净的预览界面。

---

## 四、Univer 实例初始化

```js
const { univerAPI } = createUniver({
    locale: LocaleType.ZH_CN,
    locales: {
        [LocaleType.ZH_CN]: mergeLocales(UniverPresetSheetsCoreZhCN, drawingLocale),
    },
    presets: [
        UniverSheetsCorePreset({ toolbar: false, formulaBar: false, contextMenu: false }),
        UniverSheetsDrawingPreset(),
    ],
});
```

**配置说明：**

| 配置项 | 值 | 作用 |
|--------|-----|------|
| `locale` | `ZH_CN` | 界面语言为简体中文 |
| `toolbar` | `false` | 隐藏工具栏（纯预览不需要） |
| `formulaBar` | `false` | 隐藏公式栏 |
| `contextMenu` | `false` | 禁用右键菜单 |
| `UniverSheetsDrawingPreset` | 启用 | 支持图片渲染 |

`createUniver()` 执行后，Univer 在 `#app` 容器中渲染一个空白电子表格界面，等待数据填充。

---

## 五、文件下载与入口

页面通过 URL 参数获取文件地址，自动下载并触发解析：

```js
const params = new URLSearchParams(location.search);
const fileUrl = params.get('url');

const response = await fetch(fileUrl);
const arrayBuffer = await response.arrayBuffer();
const fileName = decodeURIComponent(fileUrl.split('/').pop().split('?')[0]);
const file = new File([arrayBuffer], fileName, {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
});
await importFile(file);
```

**流程：**
1. 从 `?url=` 参数提取文件 URL
2. `fetch()` 下载文件为 `ArrayBuffer`
3. 从 URL 中提取文件名（`decodeURIComponent` 处理中文名）
4. 构造 `File` 对象（LuckyExcel 需要 File 类型输入）
5. 调用 `importFile()` 进入解析流程

---

## 六、主解析器：LuckyExcel

### 6.1 调用方式

```js
LuckyExcel.transformExcelToUniver(file, successCallback, errorCallback);
```

这是一个基于回调的异步 API，代码中用 Promise 包装并加了 15 秒超时保护：

```js
const luckyPromise = new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error('超时')), 15000);
    LuckyExcel.transformExcelToUniver(
        file,
        (univerData) => { clearTimeout(timeout); resolve(univerData); },
        (error) => { clearTimeout(timeout); reject(error); }
    );
});
```

### 6.2 内部解析流程

```
xlsx 文件 (File 对象)
    ↓ JSZip 解压 ZIP 包
ZIP 包内的 XML 文件
    ├── [Content_Types].xml       → 文件类型声明
    ├── xl/workbook.xml           → Sheet 列表和顺序
    ├── xl/sharedStrings.xml      → 共享字符串表（所有文本值的去重池）
    ├── xl/styles.xml             → 样式定义（字体/填充/边框/数字格式）
    ├── xl/worksheets/sheet1.xml  → 各 Sheet 的单元格数据
    │   └── <row r="1">
    │       └── <c r="A1" s="0" t="s">  ← s=样式索引, t=类型(s=字符串引用)
    │           └── <v>0</v>             ← 值（此处为 sharedStrings 索引）
    ├── xl/drawings/...           → 图片引用关系
    └── xl/media/...              → 图片二进制数据
    ↓ XML 解析 + 数据映射
LuckySheet 中间格式
    ↓ LuckyToUniver 转换器
Univer IWorkbookData 格式（直接可用于 createWorkbook）
```

### 6.3 LuckyExcel 输出的数据结构

`transformExcelToUniver` 的回调参数 `univerData` 是一个完整的 `IWorkbookData` 对象：

```js
{
  id: 'workbook-xxx',
  name: '文件名',
  locale: 'zh-CN',
  styles: {
    'style_1': { ff: '宋体', fs: 11, bl: 1, bg: { rgb: '#FFFFFF' }, ... },
    ...
  },
  sheetOrder: ['sheet_id_1', 'sheet_id_2'],
  sheets: {
    'sheet_id_1': {
      id: 'sheet_id_1',
      name: '养老',
      cellData: {
        0: {  // 第1行
          0: { v: '序号', t: 1, s: 'style_1' },     // A1
          1: { v: '姓名', t: 1, s: 'style_1' },     // B1
          ...
        },
        1: {  // 第2行
          0: { v: 1, t: 2 },                          // A2: 数字
          1: { v: '董继芬', t: 1, s: 'style_2' },    // B2: 文本+样式
          ...
        }
      },
      mergeData: [
        { startRow: 0, startColumn: 0, endRow: 0, endColumn: 3 }  // 合并区域
      ],
      rowData: { 0: { h: 25 }, 1: { h: 20 } },       // 行高
      columnData: { 0: { w: 50 }, 1: { w: 80 } },     // 列宽
      ...
    }
  }
}
```

### 6.4 文本校正机制 (fixCellValues)

LuckyExcel 转换后，部分单元格的显示值可能与源文件不一致（尤其是富文本单元格的 `dataStream` 字段）。代码使用 ExcelJS 重新读取文件进行逐单元格校正：

```
LuckyExcel 输出的 univerData
       ↓
ExcelJS 重新解析同一个 xlsx 文件
       ↓
按 Sheet 名匹配，逐行逐列对比
       ↓
对于每个有 p.body.dataStream 的富文本单元格：
  ├── 读取 ExcelJS 解析的"期望值"
  ├── 与 LuckyExcel 的 dataStream 对比
  └── 不一致时修正以下字段：
      ├── p.body.dataStream  → 替换为正确文本 + "\r\n"
      ├── p.body.paragraphs  → 重置段落索引
      ├── p.body.sectionBreaks → 重置分节符索引
      ├── p.body.textRuns    → 重置文本运行范围 (st=0, ed=新长度)
      ├── p.body.customRanges → 重置自定义范围
      └── v, t               → 更新值和类型
```

**校正的核心逻辑：**

```js
// 富文本单元格的 dataStream 格式：文本内容 + "\r\n" 结尾
const ds = univerCell.p.body.dataStream.replace(/\r?\n$/, '');
if (ds !== expectedStr) {
    univerCell.p.body.dataStream = expectedStr + '\r\n';
    // 同步更新所有索引...
}
```

---

## 七、备用解析器：ExcelJS

当 LuckyExcel 失败或超时时自动触发，也在 LuckyExcel 不可用时直接使用。

### 7.1 解析入口

```js
const workbook = new ExcelJS.Workbook();
await workbook.xlsx.load(arrayBuffer);  // 解析 xlsx 二进制数据
```

### 7.2 ExcelJS 到 Univer 的数据转换

ExcelJS 解析出的是自己的数据模型，需要手动转换为 Univer 的 `IWorkbookData` 格式。转换分为以下几个部分：

#### 7.2.1 单元格值转换 (getCellDisplayValue)

ExcelJS 的单元格值类型多样，需要统一映射为 Univer 的 `{ v, t }` 格式：

| ExcelJS 值类型 | Univer 映射 | 说明 |
|----------------|-------------|------|
| `number` | `{ v: 数值, t: 2 }` | 数字类型 |
| `string` | `{ v: '文本', t: 1 }` | 字符串类型 |
| `boolean` | `{ v: 1或0, t: 3 }` | 布尔转为 0/1 |
| `Date` | `{ v: Excel序列号, t: 2 }` | 转为 Excel 日期序列号（从 1899-12-30 起算的天数） |
| `{ richText: [...] }` | `{ v: '拼接文本', t: 1 }` | 富文本提取纯文本 |
| `{ formula, result }` | `{ v: result, t: 根据类型 }` | 公式取计算结果 |
| `{ text }` | `{ v: '文本', t: 1 }` | 超链接等复合值 |
| `{ error }` | `{ v: '#ERROR!', t: 1 }` | 错误值 |

**日期转换公式：**
```js
const epoch = new Date(1899, 11, 30);  // Excel 纪元
const excelSerialNumber = (date.getTime() - epoch.getTime()) / 86400000;
```

#### 7.2.2 颜色解析 (resolveColor)

Excel 文件中的颜色有三种编码方式，需要统一转为 `#RRGGBB` 格式：

```
resolveColor(color)
    ├── color.argb 存在？
    │   └── convertArgbColor: "FF4472C4" → "#4472C4"（去掉 Alpha 通道）
    │
    ├── color.indexed 存在？
    │   └── 查 INDEXED_COLORS 表：indexed=2 → "#FF0000"
    │       （Excel 预定义的 64 色调色板）
    │
    └── color.theme 存在？
        └── 查 THEME_COLORS 表：theme=3 → "#4472C4"
            └── 有 tint？→ applyTint 计算色调偏移
                tint > 0：向白色偏移（变浅）
                tint < 0：向黑色偏移（变深）
```

**ARGB 颜色转换：**
```
输入: "FF4472C4" (8位，含Alpha)  → 输出: "#4472C4"
输入: "4472C4"   (6位，纯RGB)    → 输出: "#4472C4"
输入: "#FF4472C4"(带#号9位)      → 输出: "#4472C4"
```

**色调偏移算法 (applyTint)：**
```
tint > 0（变浅）: newR = R + (255 - R) × tint
tint < 0（变深）: newR = R × (1 + tint)
```

#### 7.2.3 样式转换 (buildUniverStyle)

将 ExcelJS 的样式对象转换为 Univer 样式格式：

| ExcelJS 属性 | Univer 字段 | 说明 |
|-------------|-------------|------|
| `font.name` | `ff` | 字体族 (font family) |
| `font.size` | `fs` | 字号 (font size) |
| `font.bold` | `bl` | 粗体 (bold)，1=启用 |
| `font.italic` | `it` | 斜体 (italic)，1=启用 |
| `font.underline` | `ul.s` | 下划线，`{ s: 1 }` |
| `font.strike` | `st.s` | 删除线，`{ s: 1 }` |
| `font.color` | `cl.rgb` | 字体颜色 |
| `fill (pattern=solid)` | `bg.rgb` | 背景填充色（仅 solid 纯色填充生效） |
| `fill (gradient)` | `bg.rgb` | 渐变取第一个色标 |
| `border.top/bottom/left/right` | `bd.t/b/l/r` | 边框（样式+颜色） |
| `alignment.horizontal` | `ht` | 水平对齐：1=左, 2=居中, 3=右, 4=两端 |
| `alignment.vertical` | `vt` | 垂直对齐：1=上, 2=居中, 3=下 |
| `alignment.wrapText` | `tb` | 自动换行，3=启用 |
| `alignment.textRotation` | `tr.a` | 文本旋转角度 |
| `numFmt` | `n.pattern` | 数字格式模式串 |

**填充色处理的关键逻辑：**

```js
// 只有 pattern === 'solid' 时才应用背景色
// gray125、gray0625 等图案填充不作为纯色背景处理
if (fill.type === 'pattern' && fill.pattern === 'solid' && fill.fgColor) {
    const c = resolveColor(fill.fgColor);
    if (c) { s.bg = { rgb: c }; }
}
```

> **为什么只处理 solid？** Excel 中 `gray125` 等图案填充在 xlsx 文件中也会携带 `fgColor`，但它表示的是图案前景色而非纯色背景。如果不加 `pattern === 'solid'` 判断，会导致本应无底色的单元格显示出错误的背景色。

#### 7.2.4 边框转换

```
ExcelJS 边框样式 → Univer 边框样式编号：

thin=1, hair=2, dotted=3, dashed=4, dashDot=5,
dashDotDot=6, double=7, medium=8, mediumDashed=9,
mediumDashDot=10, mediumDashDotDot=11, slantDashDot=12, thick=13
```

每条边框转换为：`{ s: 样式编号, cl: { rgb: '#颜色' } }`

#### 7.2.5 合并单元格解析 (parseMerges)

ExcelJS 的合并信息存储在 `worksheet.model.merges` 中，格式为 Excel 引用字符串：

```
"A1:D1" → { startRow: 0, startColumn: 0, endRow: 0, endColumn: 3 }
"B3:C5" → { startRow: 2, startColumn: 1, endRow: 4, endColumn: 2 }
```

列字母转数字索引：`A=0, B=1, ..., Z=25, AA=26, AB=27, ...`

```js
function colLetterToIndex(letters) {
    let idx = 0;
    for (let i = 0; i < letters.length; i++)
        idx = idx * 26 + (letters.charCodeAt(i) - 64);
    return idx - 1;
}
```

#### 7.2.6 行列尺寸转换

```js
// 列宽：ExcelJS 的字符宽度 × 7.5 ≈ 像素宽度
columnData[colIdx] = { w: Math.round(col.width * 7.5), hd: 0 };

// 行高：ExcelJS 直接提供像素值
rowData[rowIndex - 1] = { h: row.height, hd: 0 };
```

#### 7.2.7 图片处理

```
ExcelJS 解析出图片列表
    ↓
遍历每个 worksheet 的 getImages()
    ↓
通过 imageId 从 workbook.media 获取图片二进制数据
    ↓
转为 Base64 Data URL: "data:image/png;base64,..."
    ↓
从 img.range 提取位置信息：
    ├── tl (top-left): 左上角的行列坐标
    ├── ext: 明确的宽高像素值
    └── br (bottom-right): 右下角坐标（用于计算宽高）
    ↓
通过 Univer Drawing API 插入：
    fSheet.newOverGridImage()
        .setSource(dataUrl, ImageSourceType.URL)
        .setColumn(col).setRow(row)
        .setWidth(width).setHeight(height)
        .buildAsync()
    ↓
    fSheet.insertImages([image])
```

#### 7.2.8 样式去重机制

为避免大量重复样式对象，使用 JSON 序列化作为去重 key：

```js
const styleKey = JSON.stringify(univerStyle);
if (!styleMap[styleKey]) {
    const sid = 'style_' + (styleIdCounter++);
    styleMap[styleKey] = sid;
    styles[sid] = univerStyle;
}
cellObj.s = styleMap[styleKey];  // 单元格引用样式 ID
```

这样相同样式的单元格共享同一个样式 ID，减少内存占用。

### 7.3 最终组装

```js
const workbookData = {
    id: 'workbook-' + Date.now(),
    name: fileName,
    appVersion: '0.10.2',
    locale: LocaleType.ZH_CN,
    styles,           // 去重后的样式表
    sheetOrder,       // Sheet ID 有序数组
    sheets: sheetData, // 所有 Sheet 的数据
    resources: [],
};
const fWorkbook = univerAPI.createWorkbook(workbookData);
```

---

## 八、只读保护机制

文件加载成功后，通过**双重保护**确保用户无法编辑：

### 8.1 Workbook 级别

```js
const perm = fWorkbook.getWorkbookPermission();
await perm.setPoint(WorkbookPermissionPoint.Edit, false);
```

### 8.2 Worksheet 级别

```js
for (const sheet of fWorkbook.getSheets()) {
    const wsPerm = sheet.getWorksheetPermission();
    await wsPerm.protect({});  // 启用工作表保护
    await wsPerm.setPoint(WorksheetPermissionPoint.Edit, false);
}
```

两层保护都有 `try/catch` 包裹，任一层失败不影响另一层。

---

## 九、错误处理设计

代码中有多层错误捕获，确保任何异常都不会导致页面卡死：

| 层级 | 捕获位置 | 处理方式 |
|------|----------|----------|
| URL 参数检查 | 缺少 `?url=` 参数 | 显示提示文字 |
| 文件下载失败 | `fetch()` 返回非 200 | 显示错误信息 |
| 外层 `try/catch` | 包裹整个导入流程 | 弹出 alert 提示 |
| LuckyExcel Promise | 主解析器的成功/失败/超时 | 失败时自动回退到 ExcelJS |
| ExcelJS `try/catch` | 备用解析器 | 两个引擎都失败才弹出 alert |
| 图片插入 `try/catch` | 每张图片单独捕获 | 单张图片失败不影响整体 |
| 文本校正 `try/catch` | fixCellValues 整体捕获 | 校正失败不影响主流程 |
| 只读保护 `try/catch` | Workbook 和 Worksheet 分别捕获 | 保护失败仅 console.warn |

所有错误路径都确保 `showLoading(false)` 被调用，避免遮罩层卡住。

控制台日志带有 `[主解析器]`、`[备用解析器]`、`[文本校正]`、`[只读]` 等前缀，方便排查问题。

---

## 十、回退触发条件

| 条件 | 说明 |
|------|------|
| LuckyExcel 内部抛出异常 | 如 `Cannot read properties of null (reading 'split')` |
| LuckyExcel 错误回调触发 | 库主动报告解析失败 |
| 解析超时 15 秒 | 防止某些文件导致无限等待 |
| LuckyExcel CDN 加载失败 | `window.LuckyExcel` 不存在，直接使用 ExcelJS |
| 非 File 对象输入 | LuckyExcel 需要 File 类型，ArrayBuffer 直接走 ExcelJS |

---

## 十一、转换覆盖的内容

| 类别 | LuckyExcel | ExcelJS 手动转换 |
|------|:----------:|:----------------:|
| 单元格值（字符串/数字/布尔/日期） | ✅ | ✅ |
| 公式（文本及计算结果） | ✅ | ✅（取结果值） |
| 字体样式（名/大小/粗体/斜体/下划线/删除线/颜色） | ✅ | ✅ |
| 纯色背景填充 | ✅ | ✅ |
| 图案填充（gray125 等） | ✅ | ❌（仅处理 solid） |
| 边框（样式+颜色，上下左右） | ✅ | ✅ |
| 对齐（水平/垂直/自动换行/文本旋转） | ✅ | ✅ |
| 数字格式（日期/百分比/货币等） | ✅ | ✅ |
| 合并单元格 | ✅ | ✅ |
| 行高/列宽 | ✅ | ✅ |
| 多 Sheet 及顺序 | ✅ | ✅ |
| 嵌入图片 | ✅ | ✅ |
| 富文本 | ✅ | ✅（提取纯文本） |

---

## 十二、Univer 数据结构参考

> 官方文档：
> - https://docs.univer.ai/guides/sheets/model/workbook-data
> - https://docs.univer.ai/guides/sheets/model/worksheet-data
> - https://docs.univer.ai/guides/sheets/model/cell-data

### IWorkbookData（工作簿）

```js
{
  id: 'workbook-1710000000000',
  name: '社保明细.xlsx',
  appVersion: '0.10.2',
  locale: 'zh-CN',
  styles: {
    'style_1': { ff: '宋体', fs: 11, bl: 1, cl: { rgb: '#000000' } },
    'style_2': { bg: { rgb: '#4472C4' }, ht: 2, vt: 2 },
  },
  sheetOrder: ['sheet_1', 'sheet_2'],
  sheets: { /* 各 Sheet 数据 */ },
  resources: [],
}
```

### IWorksheetData（工作表）

```js
{
  id: 'sheet_1',
  name: '养老',
  tabColor: '',
  hidden: 0,
  freeze: { xSplit: 0, ySplit: 0, startRow: -1, startColumn: -1 },
  rowCount: 1000,
  columnCount: 26,
  defaultColumnWidth: 73,
  defaultRowHeight: 23,
  mergeData: [{ startRow: 0, startColumn: 0, endRow: 0, endColumn: 8 }],
  cellData: { /* 行号: { 列号: ICellData } */ },
  rowData: { 0: { h: 25, hd: 0 } },
  columnData: { 0: { w: 50, hd: 0 } },
  rowHeader: { width: 46, hidden: 0 },
  columnHeader: { height: 20, hidden: 0 },
  showGridlines: 1,
  rightToLeft: 0,
}
```

### ICellData（单元格）

```js
{
  v: '董继芬',        // 显示值
  t: 1,              // 类型：1=字符串, 2=数字, 3=布尔
  s: 'style_1',      // 样式 ID 引用
  f: '=SUM(H3:H8)',  // 公式（可选）
  p: {               // 富文本（可选，LuckyExcel 转换时生成）
    body: {
      dataStream: '董继芬\r\n',
      textRuns: [{ st: 0, ed: 3, ts: { ff: '宋体', fs: 11 } }],
      paragraphs: [{ startIndex: 3 }],
    }
  }
}
```

### 样式对象字段速查

| 字段 | 含义 | 示例值 |
|------|------|--------|
| `ff` | 字体族 | `'宋体'` |
| `fs` | 字号 | `11` |
| `bl` | 粗体 | `1` |
| `it` | 斜体 | `1` |
| `ul` | 下划线 | `{ s: 1 }` |
| `st` | 删除线 | `{ s: 1 }` |
| `cl` | 字体颜色 | `{ rgb: '#FF0000' }` |
| `bg` | 背景色 | `{ rgb: '#FFFFFF' }` |
| `bd` | 边框 | `{ t: {s:1, cl:{rgb:'#000'}}, b: {...}, l: {...}, r: {...} }` |
| `ht` | 水平对齐 | `1=左, 2=中, 3=右, 4=两端` |
| `vt` | 垂直对齐 | `1=上, 2=中, 3=下` |
| `tb` | 文本换行 | `3=自动换行` |
| `tr` | 文本旋转 | `{ a: 45, v: 0 }` |
| `n` | 数字格式 | `{ pattern: '0.00%' }` |

---

## 十三、方案选型历程

| 阶段 | 方案 | 问题 |
|------|------|------|
| v1 | ExcelJS 解析 + 手写转换代码 → Univer | 样式丢失、图片不显示、颜色不对 |
| v2 | `@univerjs/preset-sheets-advanced` 官方导入 | 依赖商业版包，CDN 模式不可用 |
| v3 | `@mertdeveci55/univer-import-export` 单引擎 | 部分 xlsx 触发内部 bug |
| **v4（当前）** | **LuckyExcel 主解析 + ExcelJS 校正/回退** | **兼顾质量和兼容性** |

---

## 十四、关键第三方库参考

| 库 | 版本 | 用途 | 文档 |
|----|------|------|------|
| UniverJS | latest (CDN) | 电子表格渲染引擎 | https://docs.univer.ai/ |
| @mertdeveci55/univer-import-export | 0.2.1 | 主解析器 | https://www.npmjs.com/package/@mertdeveci55/univer-import-export |
| ExcelJS | latest (CDN) | 备用解析器 + 文本校正 | https://github.com/exceljs/exceljs |
| React | 18.3.1 | Univer UI 依赖 | https://react.dev/ |
| RxJS | latest | Univer 数据流依赖 | https://rxjs.dev/ |
