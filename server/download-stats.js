#!/usr/bin/env node
/*
 * LiteSchedule 下载统计（独立轻量服务）
 *
 * 作用：
 *   - 统计 APK 下载次数（总量 + 按天）
 *   - 提供仅管理员可查看的统计接口
 *
 * 默认路径假设：
 *   - 配置：/opt/qk-server/config.json（读取 adminToken）
 *   - 数据：/opt/qk-server/downloads.json
 *   - APK 目录：/var/www/schedule.hoshichan.moe/html
 *
 * 可以通过环境变量覆盖：
 *   QK_CONFIG          配置文件路径
 *   QK_DOWNLOADS       统计数据文件路径
 *   QK_DOWNLOAD_DIR    APK 所在目录
 *   QK_DOWNLOAD_PORT   监听端口，默认 8766
 *   QK_ADMIN_TOKEN     管理令牌（不配置时从 config.json 读）
 *   QK_DOWNLOAD_BASE   如果不想由本服务直接发文件，可以填静态文件基础 URL，
 *                      统计后 302 跳转到该地址。
 */

'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const DEFAULT_CONFIG = '/opt/qk-server/config.json';
const DEFAULT_DATA = '/opt/qk-server/downloads.json';
const DEFAULT_DOWNLOAD_DIR = '/var/www/schedule.hoshichan.moe/html';
const DEFAULT_PORT = 8766;

function loadConfig() {
  const p = process.env.QK_CONFIG || DEFAULT_CONFIG;
  try {
    return JSON.parse(fs.readFileSync(p, 'utf8'));
  } catch (e) {
    return {};
  }
}

function loadDownloads(file) {
  try {
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (!data.files) data.files = {};
    if (!data.history) data.history = {};
    return data;
  } catch (e) {
    return { files: {}, history: {}, lastUpdated: null };
  }
}

function saveDownloads(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
  fs.renameSync(tmp, file);
}

function today() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function lastNDays(n) {
  const days = [];
  const now = new Date();
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    days.push(`${y}-${m}-${day}`);
  }
  return days;
}

function recordDownload(data, filename, date) {
  if (!data.files[filename]) {
    data.files[filename] = { total: 0, daily: {} };
  }
  const file = data.files[filename];
  file.total += 1;
  file.daily[date] = (file.daily[date] || 0) + 1;
  data.history[date] = (data.history[date] || 0) + 1;
  data.lastUpdated = new Date().toISOString();
  return file.total;
}

function statsPayload(data) {
  let grandTotal = 0;
  const files = {};
  const days = lastNDays(7);

  for (const name of Object.keys(data.files || {})) {
    const f = data.files[name];
    grandTotal += f.total || 0;
    const last7 = days.reduce((sum, day) => sum + (f.daily[day] || 0), 0);
    files[name] = {
      total: f.total || 0,
      today: f.daily[today()] || 0,
      last7: last7,
      daily: f.daily || {}
    };
  }

  return {
    total: grandTotal,
    files: files,
    history: data.history || {},
    lastUpdated: data.lastUpdated || null
  };
}

function sendJson(res, code, obj) {
  const body = JSON.stringify(obj, null, 2);
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store'
  });
  res.end(body);
}

function safeName(name) {
  return path.basename(name).replace(/[^a-zA-Z0-9._-]/g, '_');
}

function isAuthorized(parsed, req, token) {
  if (!token) return false;
  if (parsed.query.token === token) return true;
  const auth = req.headers.authorization || '';
  if (auth.toLowerCase().startsWith('bearer ') && auth.slice(7).trim() === token) return true;
  return false;
}

const config = loadConfig();
const dataFile = process.env.QK_DOWNLOADS || config.downloadsFile || DEFAULT_DATA;
const downloadDir = process.env.QK_DOWNLOAD_DIR || config.downloadDir || DEFAULT_DOWNLOAD_DIR;
const port = parseInt(process.env.QK_DOWNLOAD_PORT || config.downloadPort || DEFAULT_PORT, 10);
const adminToken = process.env.QK_ADMIN_TOKEN || config.adminToken || '';
const redirectBase = process.env.QK_DOWNLOAD_BASE || config.redirectBase || '';

let data = loadDownloads(dataFile);

const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);
  let pathname = parsed.pathname || '/';
  try {
    pathname = decodeURIComponent(pathname);
  } catch (e) {
    // keep raw pathname on malformed percent-encoding
  }

  // 下载统计入口：/dl/<文件名>
  const downloadMatch = pathname.match(/^\/dl\/(.+)$/);
  if (downloadMatch && (req.method === 'GET' || req.method === 'HEAD')) {
    const filename = safeName(downloadMatch[1]);
    const filePath = path.join(downloadDir, filename);

    if (!fs.existsSync(filePath) && !redirectBase) {
      sendJson(res, 404, { error: 'file not found' });
      return;
    }

    if (req.method === 'GET') {
      recordDownload(data, filename, today());
      saveDownloads(dataFile, data);
    }

    if (redirectBase) {
      const location = redirectBase.replace(/\/$/, '') + '/' + encodeURIComponent(filename);
      res.writeHead(302, { Location: location });
      res.end();
      return;
    }

    const stat = fs.statSync(filePath);
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': stat.size,
      'Content-Disposition': 'attachment; filename="' + filename + '"',
      'Cache-Control': 'no-store'
    });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    fs.createReadStream(filePath).pipe(res);
    return;
  }

  // 仅管理员可查看的统计接口
  if (pathname === '/stats' || pathname === '/api/downloads/stats') {
    if (!isAuthorized(parsed, req, adminToken)) {
      sendJson(res, 403, { error: 'forbidden' });
      return;
    }
    sendJson(res, 200, statsPayload(data));
    return;
  }

  // 简单 HTML 查看页（带 ?token= 访问）
  if (pathname === '/' || pathname === '/stats.html') {
    if (!isAuthorized(parsed, req, adminToken)) {
      sendJson(res, 403, { error: 'forbidden' });
      return;
    }
    const stats = statsPayload(data);
    let rows = '';
    for (const name of Object.keys(stats.files)) {
      const f = stats.files[name];
      rows += `<tr><td>${name}</td><td>${f.total}</td><td>${f.today}</td><td>${f.last7}</td></tr>`;
    }
    if (!rows) {
      rows = '<tr><td colspan="4">暂无下载记录</td></tr>';
    }

    let historyRows = '';
    const days = Object.keys(stats.history).sort().reverse();
    for (const day of days.slice(0, 30)) {
      historyRows += `<tr><td>${day}</td><td>${stats.history[day]}</td></tr>`;
    }
    if (!historyRows) {
      historyRows = '<tr><td colspan="2">暂无历史记录</td></tr>';
    }

    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end(`<!doctype html><html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>LiteSchedule 下载统计</title>
<body style="font-family: sans-serif; padding: 24px; background:#f4f6fb; color:#2d3142">
<h2>LiteSchedule 下载统计</h2>
<p>总下载：<b>${stats.total}</b></p>
<h3>分文件</h3>
<table border="1" cellpadding="6" style="border-collapse:collapse;background:#fff">
<tr><th>文件</th><th>总下载</th><th>今天</th><th>最近7天</th></tr>
${rows}
</table>
<h3>最近每日下载</h3>
<table border="1" cellpadding="6" style="border-collapse:collapse;background:#fff">
<tr><th>日期</th><th>下载量</th></tr>
${historyRows}
</table>
<p style="color:#6b7280">更新时间：${stats.lastUpdated || '暂无'}</p>
</body></html>`);
    return;
  }

  sendJson(res, 404, { error: 'not found' });
});

server.listen(port, () => {
  console.log(`[download-stats] listening on 0.0.0.0:${port}`);
  console.log(`[download-stats] data file: ${dataFile}`);
  console.log(`[download-stats] apk dir: ${downloadDir}`);
});
