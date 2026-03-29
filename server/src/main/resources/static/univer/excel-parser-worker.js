'use strict';

importScripts('lib/js/luckyexcel.umd.js', 'lib/js/exceljs.min.js');

// ========== ExcelJS 解析工具函数 ==========

const INDEXED_COLORS = [
    '#000000','#FFFFFF','#FF0000','#00FF00','#0000FF','#FFFF00','#FF00FF','#00FFFF',
    '#000000','#FFFFFF','#FF0000','#00FF00','#0000FF','#FFFF00','#FF00FF','#00FFFF',
    '#800000','#008000','#000080','#808000','#800080','#008080','#C0C0C0','#808080',
    '#9999FF','#993366','#FFFFCC','#CCFFFF','#660066','#FF8080','#0066CC','#CCCCFF',
    '#000080','#FF00FF','#FFFF00','#00FFFF','#800080','#800000','#008080','#0000FF',
    '#00CCFF','#CCFFFF','#CCFFCC','#FFFF99','#99CCFF','#FF99CC','#CC99FF','#FFCC99',
    '#3366FF','#33CCCC','#99CC00','#FFCC00','#FF9900','#FF6600','#666699','#969696',
    '#003366','#339966','#003300','#333300','#993300','#993366','#333399','#333333',
];

const THEME_COLORS = [
    '#000000','#FFFFFF','#44546A','#4472C4',
    '#ED7D31','#A5A5A5','#FFC000','#5B9BD5',
    '#70AD47','#FF0000'
];

function convertArgbColor(argb) {
    if (!argb) return null;
    const s = String(argb);
    if (s.startsWith('#')) return s.length > 7 ? '#' + s.substring(3) : s;
    if (s.length === 8) return '#' + s.substring(2);
    if (s.length === 6) return '#' + s;
    return '#' + s;
}

function applyTint(hex, tint) {
    let r = parseInt(hex.slice(1, 3), 16);
    let g = parseInt(hex.slice(3, 5), 16);
    let b = parseInt(hex.slice(5, 7), 16);
    if (tint > 0) { r = Math.round(r + (255 - r) * tint); g = Math.round(g + (255 - g) * tint); b = Math.round(b + (255 - b) * tint); }
    else { r = Math.round(r * (1 + tint)); g = Math.round(g * (1 + tint)); b = Math.round(b * (1 + tint)); }
    r = Math.min(255, Math.max(0, r)); g = Math.min(255, Math.max(0, g)); b = Math.min(255, Math.max(0, b));
    return '#' + [r, g, b].map(c => c.toString(16).padStart(2, '0')).join('');
}

function resolveColor(color) {
    if (!color) return null;
    if (color.argb) return convertArgbColor(color.argb);
    if (color.indexed !== undefined && color.indexed < INDEXED_COLORS.length) return INDEXED_COLORS[color.indexed];
    if (color.theme !== undefined) {
        let base = THEME_COLORS[color.theme] || '#000000';
        return color.tint ? applyTint(base, color.tint) : base;
    }
    return null;
}

function convertBorderStyle(s) {
    const map = { thin:1, hair:2, dotted:3, dashed:4, dashDot:5, dashDotDot:6, double:7, medium:8, mediumDashed:9, mediumDashDot:10, mediumDashDotDot:11, slantDashDot:12, thick:13 };
    return map[s] !== undefined ? map[s] : 1;
}

function convertBorderSide(side) {
    if (!side || !side.style) return null;
    return { s: convertBorderStyle(side.style), cl: { rgb: resolveColor(side.color) || '#000000' } };
}

function convertBorder(b) {
    if (!b) return undefined;
    const bd = {}; let has = false;
    if (b.top)    { const s = convertBorderSide(b.top);    if (s) { bd.t = s; has = true; } }
    if (b.bottom) { const s = convertBorderSide(b.bottom); if (s) { bd.b = s; has = true; } }
    if (b.left)   { const s = convertBorderSide(b.left);   if (s) { bd.l = s; has = true; } }
    if (b.right)  { const s = convertBorderSide(b.right);  if (s) { bd.r = s; has = true; } }
    return has ? bd : undefined;
}

function buildUniverStyle(cs) {
    if (!cs) return null;
    const s = {}; let has = false;
    if (cs.font) {
        const f = cs.font;
        if (f.name) { s.ff = f.name; has = true; }
        if (f.size) { s.fs = f.size; has = true; }
        if (f.bold) { s.bl = 1; has = true; }
        if (f.italic) { s.it = 1; has = true; }
        if (f.underline) { s.ul = { s: 1 }; has = true; }
        if (f.strike) { s.st = { s: 1 }; has = true; }
        if (f.color) { const c = resolveColor(f.color); if (c) { s.cl = { rgb: c }; has = true; } }
    }
    if (cs.fill) {
        const fill = cs.fill;
        if (fill.type === 'pattern' && fill.pattern === 'solid' && fill.fgColor) {
            const c = resolveColor(fill.fgColor);
            if (c) { s.bg = { rgb: c }; has = true; }
        } else if (fill.type === 'gradient' && fill.stops && fill.stops.length > 0) {
            const c = resolveColor(fill.stops[0].color);
            if (c) { s.bg = { rgb: c }; has = true; }
        }
    }
    const bd = convertBorder(cs.border);
    if (bd) { s.bd = bd; has = true; }
    if (cs.alignment) {
        const a = cs.alignment;
        const htMap = { left:1, center:2, centerContinuous:2, right:3, fill:1, justify:4, distributed:4 };
        const vtMap = { top:1, middle:2, center:2, bottom:3, justify:2, distributed:2 };
        if (a.horizontal && htMap[a.horizontal]) { s.ht = htMap[a.horizontal]; has = true; }
        if (a.vertical && vtMap[a.vertical]) { s.vt = vtMap[a.vertical]; has = true; }
        if (a.wrapText) { s.tb = 3; has = true; }
        if (a.textRotation) { s.tr = { a: a.textRotation, v: 0 }; has = true; }
    }
    if (cs.numFmt) { s.n = { pattern: cs.numFmt }; has = true; }
    return has ? s : null;
}

function getCellDisplayValue(cell) {
    if (cell.formula) {
        const result = cell.result;
        if (result !== null && result !== undefined) {
            if (typeof result === 'number') return { v: result, t: 2 };
            return { v: String(result), t: 1 };
        }
    }
    let value = cell.value;
    if (value === null || value === undefined) return { v: '' };
    if (typeof value === 'number') return { v: value, t: 2 };
    if (typeof value === 'boolean') return { v: value ? 1 : 0, t: 3 };
    if (value instanceof Date) {
        const epoch = new Date(1899, 11, 30);
        return { v: (value.getTime() - epoch.getTime()) / 86400000, t: 2 };
    }
    if (typeof value === 'object') {
        if (value.richText && Array.isArray(value.richText)) return { v: value.richText.map(rt => rt.text || '').join(''), t: 1 };
        if (value.result !== undefined) return typeof value.result === 'number' ? { v: value.result, t: 2 } : { v: String(value.result), t: 1 };
        if (value.text !== undefined) {
            const textVal = value.text;
            if (typeof textVal === 'object' && textVal.richText) {
                return { v: textVal.richText.map(rt => rt.text || '').join(''), t: 1 };
            }
            return { v: String(textVal), t: 1 };
        }
        if (value.error) return { v: String(value.error), t: 1 };
        if (value.sharedFormula) return { v: '', t: 1 };
        return { v: JSON.stringify(value), t: 1 };
    }
    return { v: String(value), t: 1 };
}

function colLetterToIndex(letters) {
    let idx = 0;
    for (let i = 0; i < letters.length; i++) idx = idx * 26 + (letters.charCodeAt(i) - 64);
    return idx - 1;
}

function parseMerges(ws) {
    const merges = [];
    if (!ws.model || !ws.model.merges) return merges;
    ws.model.merges.forEach(ref => {
        const m = ref.match(/^([A-Z]+)(\d+):([A-Z]+)(\d+)$/);
        if (m) merges.push({ startRow: parseInt(m[2]) - 1, startColumn: colLetterToIndex(m[1]), endRow: parseInt(m[4]) - 1, endColumn: colLetterToIndex(m[3]) });
    });
    return merges;
}

function arrayBufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const chunkSize = 8192;
    for (let i = 0; i < bytes.byteLength; i += chunkSize) {
        const end = Math.min(i + chunkSize, bytes.byteLength);
        for (let j = i; j < end; j++) binary += String.fromCharCode(bytes[j]);
    }
    return self.btoa(binary);
}

// ========== ExcelJS 完整解析 ==========

function parseWithExcelJS(arrayBuffer, fileName) {
    const workbook = new ExcelJS.Workbook();

    return workbook.xlsx.load(arrayBuffer).then(function () {
        const validWorksheets = workbook.worksheets.filter(ws => ws != null);
        const totalSheets = validWorksheets.length;
        self.postMessage({ type: 'progress', percent: 45 });

        const sheetData = {};
        const sheetOrder = [];
        const styles = {};
        const styleMap = {};
        let styleIdCounter = 1;
        const imageDataList = [];

        validWorksheets.forEach((ws, wsIndex) => {
            if (ws.getImages && ws.getImages().length > 0) {
                const imgs = ws.getImages();
                for (const img of imgs) {
                    try {
                        const mediaItem = workbook.media[img.imageId];
                        if (!mediaItem || !mediaItem.buffer) continue;
                        const ext = mediaItem.extension || 'png';
                        const mimeMap = { png:'image/png', jpg:'image/jpeg', jpeg:'image/jpeg', gif:'image/gif', bmp:'image/bmp' };
                        const dataUrl = 'data:' + (mimeMap[ext] || 'image/png') + ';base64,' + arrayBufferToBase64(mediaItem.buffer);
                        let col = 0, row = 0, width = 200, height = 200;
                        if (img.range) {
                            if (img.range.tl) { col = Math.floor(img.range.tl.col || img.range.tl.nativeCol || 0); row = Math.floor(img.range.tl.row || img.range.tl.nativeRow || 0); }
                            if (img.range.ext) { if (img.range.ext.width) width = img.range.ext.width; if (img.range.ext.height) height = img.range.ext.height; }
                            else if (img.range.br) {
                                const ec = Math.floor(img.range.br.col || img.range.br.nativeCol || col + 3);
                                const er = Math.floor(img.range.br.row || img.range.br.nativeRow || row + 5);
                                width = Math.max(100, (ec - col) * 73); height = Math.max(50, (er - row) * 23);
                            }
                        }
                        imageDataList.push({ wsIndex, dataUrl, col, row, width, height });
                    } catch (imgErr) {
                        console.warn('[Worker][ExcelJS] 图片提取失败:', imgErr);
                    }
                }
            }
        });

        validWorksheets.forEach((worksheet, index) => {
            var sheetPercent = 45 + Math.round(((index + 1) / totalSheets) * 20);
            self.postMessage({ type: 'progress', percent: sheetPercent });

            const sheetId = 'sheet_' + (index + 1);
            const cellData = {};
            const rowData = {};
            const columnData = {};
            const mergeData = parseMerges(worksheet);

            if (worksheet.columns) {
                worksheet.columns.forEach((col, colIdx) => {
                    if (col.width) columnData[colIdx] = { w: Math.round(col.width * 7.5), hd: 0 };
                });
            }

            worksheet.eachRow({ includeEmpty: true }, (row, rowIndex) => {
                const rowCells = {};
                if (row.height) rowData[rowIndex - 1] = { h: row.height, hd: 0 };

                row.eachCell({ includeEmpty: true }, (cell, colIndex) => {
                    const hasValue = cell.value !== null && cell.value !== undefined;
                    const hasStyle = cell.style && Object.keys(cell.style).length > 0;
                    if (!hasValue && !hasStyle) return;

                    const cellInfo = getCellDisplayValue(cell);
                    const cellObj = {};
                    if (hasValue) { cellObj.v = cellInfo.v; if (cellInfo.t) cellObj.t = cellInfo.t; }

                    if (hasStyle) {
                        const univerStyle = buildUniverStyle(cell.style);
                        if (univerStyle) {
                            const styleKey = JSON.stringify(univerStyle);
                            if (!styleMap[styleKey]) { const sid = 'style_' + (styleIdCounter++); styleMap[styleKey] = sid; styles[sid] = univerStyle; }
                            cellObj.s = styleMap[styleKey];
                        }
                    }
                    if (Object.keys(cellObj).length > 0) rowCells[colIndex - 1] = cellObj;
                });
                if (Object.keys(rowCells).length > 0) cellData[rowIndex - 1] = rowCells;
            });

            sheetData[sheetId] = {
                id: sheetId, name: worksheet.name, tabColor: '', hidden: 0,
                freeze: { xSplit: 0, ySplit: 0, startRow: -1, startColumn: -1 },
                rowCount: Math.max(1000, worksheet.rowCount + 100),
                columnCount: Math.max(26, worksheet.columnCount + 10),
                defaultColumnWidth: 73, defaultRowHeight: 23,
                mergeData, cellData, rowData, columnData,
                rowHeader: { width: 46, hidden: 0 }, columnHeader: { height: 20, hidden: 0 },
                showGridlines: 1, rightToLeft: 0,
            };
            sheetOrder.push(sheetId);
        });

        return {
            workbookData: {
                id: 'workbook-' + Date.now(), name: fileName || 'preview', appVersion: '0.10.2',
                locale: 'zhCN', styles, sheetOrder, sheets: sheetData, resources: [],
            },
            imageDataList,
        };
    });
}

// ========== LuckyExcel 解析后用 ExcelJS 校正单元格文本 ==========

function fixCellValues(univerData, arrayBuffer) {
    const workbook = new ExcelJS.Workbook();

    return workbook.xlsx.load(arrayBuffer).then(function () {
        const nameToSheetId = {};
        for (const sheetId of (univerData.sheetOrder || [])) {
            const sheet = univerData.sheets[sheetId];
            if (sheet && sheet.name) nameToSheetId[sheet.name] = sheetId;
        }

        let fixCount = 0;
        workbook.worksheets.filter(ws => ws != null).forEach((ws) => {
            const sheetId = nameToSheetId[ws.name];
            if (!sheetId || !univerData.sheets[sheetId]) return;
            const cellData = univerData.sheets[sheetId].cellData;
            if (!cellData) return;

            ws.eachRow({ includeEmpty: false }, (row, rowIndex) => {
                row.eachCell({ includeEmpty: false }, (cell, colIndex) => {
                    if (cell.value === null || cell.value === undefined) return;
                    const expected = getCellDisplayValue(cell);
                    if (expected.v === '' || expected.v === null || expected.v === undefined) return;

                    const r = rowIndex - 1;
                    const c = colIndex - 1;
                    if (!cellData[r] || !cellData[r][c]) return;

                    const univerCell = cellData[r][c];
                    const expectedStr = String(expected.v);

                    if (univerCell.p && univerCell.p.body && univerCell.p.body.dataStream) {
                        const ds = univerCell.p.body.dataStream.replace(/\r?\n$/, '');
                        if (ds !== expectedStr) {
                            const newDs = expectedStr + '\r\n';
                            univerCell.p.body.dataStream = newDs;
                            const newLen = expectedStr.length;
                            if (univerCell.p.body.paragraphs) {
                                univerCell.p.body.paragraphs = [{ startIndex: newLen }];
                            }
                            if (univerCell.p.body.sectionBreaks) {
                                univerCell.p.body.sectionBreaks = [{ startIndex: newLen + 1 }];
                            }
                            if (univerCell.p.body.textRuns) {
                                univerCell.p.body.textRuns = univerCell.p.body.textRuns.map(tr => ({
                                    ...tr, st: 0, ed: newLen
                                }));
                            }
                            if (univerCell.p.body.customRanges) {
                                univerCell.p.body.customRanges = univerCell.p.body.customRanges.map(cr => ({
                                    ...cr, startIndex: 0, endIndex: newLen
                                }));
                            }
                            univerCell.v = expected.v;
                            if (expected.t) univerCell.t = expected.t;
                            fixCount++;
                        }
                    }
                });
            });
        });

        if (fixCount > 0) {
            console.log('[Worker][文本校正] 共修正', fixCount, '个单元格的显示值');
        }
        return univerData;
    });
}

// ========== LuckyExcel 解析（需要 File 对象，在 Worker 中用 Blob 模拟） ==========

function parseWithLuckyExcel(arrayBuffer, fileName) {
    return new Promise(function (resolve, reject) {
        const LuckyExcelLib = self.LuckyExcel || self.luckyexcel;
        if (!LuckyExcelLib) {
            reject(new Error('LuckyExcel 库未加载'));
            return;
        }

        self.postMessage({ type: 'progress', percent: 8 });

        var timeout = setTimeout(function () {
            reject(new Error('LuckyExcel 解析超时'));
        }, 30000);

        var blob = new Blob([arrayBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
        var file = new File([blob], fileName || 'preview.xlsx', { type: blob.type });

        try {
            LuckyExcelLib.transformExcelToUniver(
                file,
                function (univerData) {
                    clearTimeout(timeout);
                    var sheetCount = univerData.sheetOrder ? univerData.sheetOrder.length : 0;
                    console.log('[Worker][LuckyExcel] 转换成功，工作表数量:', sheetCount);
                    resolve(univerData);
                },
                function (error) {
                    clearTimeout(timeout);
                    reject(error);
                }
            );
        } catch (syncErr) {
            clearTimeout(timeout);
            reject(syncErr);
        }
    });
}

// ========== Worker 消息处理 ==========

self.addEventListener('message', function (e) {
    var data = e.data;
    if (data.type !== 'parse') return;

    var arrayBuffer = data.arrayBuffer;
    var fileName = data.fileName;
    var useLuckyExcel = data.useLuckyExcel !== false;

    var arrayBufferCopy = arrayBuffer.slice(0);

    var startTime = Date.now();

    if (useLuckyExcel) {
        self.postMessage({ type: 'progress', percent: 5 });

        parseWithLuckyExcel(arrayBuffer, fileName)
            .then(function (univerData) {
                self.postMessage({ type: 'progress', percent: 40 });
                return fixCellValues(univerData, arrayBufferCopy);
            })
            .then(function (univerData) {
                self.postMessage({ type: 'progress', percent: 80 });
                var elapsed = Date.now() - startTime;
                console.log('[Worker] LuckyExcel 解析完成，耗时:', elapsed, 'ms');
                self.postMessage({
                    type: 'result',
                    parser: 'luckyexcel',
                    workbookData: univerData,
                    imageDataList: [],
                    elapsed: elapsed,
                });
            })
            .catch(function (luckyErr) {
                console.warn('[Worker] LuckyExcel 失败，回退到 ExcelJS:', luckyErr.message || luckyErr);
                self.postMessage({ type: 'progress', percent: 15 });

                parseWithExcelJS(arrayBufferCopy, fileName)
                    .then(function (result) {
                        self.postMessage({ type: 'progress', percent: 80 });
                        var elapsed = Date.now() - startTime;
                        console.log('[Worker] ExcelJS 回退解析完成，耗时:', elapsed, 'ms');
                        self.postMessage({
                            type: 'result',
                            parser: 'exceljs-fallback',
                            workbookData: result.workbookData,
                            imageDataList: result.imageDataList,
                            elapsed: elapsed,
                        });
                    })
                    .catch(function (fallbackErr) {
                        self.postMessage({ type: 'error', message: '文件解析失败: ' + (fallbackErr.message || fallbackErr) });
                    });
            });
    } else {
        self.postMessage({ type: 'progress', percent: 5 });

        parseWithExcelJS(arrayBuffer, fileName)
            .then(function (result) {
                self.postMessage({ type: 'progress', percent: 80 });
                var elapsed = Date.now() - startTime;
                console.log('[Worker] ExcelJS 解析完成，耗时:', elapsed, 'ms');
                self.postMessage({
                    type: 'result',
                    parser: 'exceljs',
                    workbookData: result.workbookData,
                    imageDataList: result.imageDataList,
                    elapsed: elapsed,
                });
            })
            .catch(function (err) {
                self.postMessage({ type: 'error', message: '文件解析失败: ' + (err.message || err) });
            });
    }
});
