const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = 8765;
const DATA_FILE = '/opt/qk-server/unlocks.json';
const CONFIG_FILE = '/opt/qk-server/config.json';
const DOWNLOADS_FILE = '/opt/qk-server/downloads.json';
const PENDING_FILE = '/opt/qk-server/pending.json';
const DOWNLOAD_DIR = '/var/www/schedule.hoshichan.moe/html';
const SECRET = 'qkb-2026-hoshi-donate-1'; // 与服务端同源的离线兼容码

function loadConfig() {
    try {
        return JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8'));
    } catch (e) {
        return { adminToken: '', paySecret: '' };
    }
}

function loadUnlocks() {
    try {
        return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    } catch (e) {
        return {};
    }
}

function saveUnlocks(data) {
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), { mode: 0o600 });
}

function loadDownloads() {
    try {
        const data = JSON.parse(fs.readFileSync(DOWNLOADS_FILE, 'utf8'));
        if (!data.files) data.files = {};
        if (!data.history) data.history = {};
        return data;
    } catch (e) {
        return { files: {}, history: {}, lastUpdated: null };
    }
}

function saveDownloads(data) {
    fs.mkdirSync(path.dirname(DOWNLOADS_FILE), { recursive: true });
    fs.writeFileSync(DOWNLOADS_FILE, JSON.stringify(data, null, 2), { mode: 0o600 });
}

// 赞赏登记队列：用户赞赏后在 App 里点「我已赞赏」，设备码进入待确认队列；
// 作者在管理页核对微信赞赏记录后一键确认，App 轮询到解锁状态后自动解锁。
function loadPending() {
    try {
        const data = JSON.parse(fs.readFileSync(PENDING_FILE, 'utf8'));
        return data.devices || {};
    } catch (e) {
        return {};
    }
}

function savePending(devices) {
    fs.mkdirSync(path.dirname(PENDING_FILE), { recursive: true });
    fs.writeFileSync(PENDING_FILE, JSON.stringify({ devices }, null, 2), { mode: 0o600 });
}

function deviceCodeValid(code) {
    return typeof code === 'string' && /^[A-F0-9]{8}$/.test(code);
}

function todayStr() {
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
}

function downloadsPayload(data) {
    let grandTotal = 0;
    const files = {};
    const days = lastNDays(7);
    const today = todayStr();

    for (const name of Object.keys(data.files || {})) {
        const f = data.files[name];
        grandTotal += f.total || 0;
        const last7 = days.reduce((sum, day) => sum + (f.daily[day] || 0), 0);
        files[name] = {
            total: f.total || 0,
            today: f.daily[today] || 0,
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

function sha256Hex(s) {
    return crypto.createHash('sha256').update(s, 'utf8').digest('hex');
}

function hmacHex(secret, s) {
    return crypto.createHmac('sha256', secret).update(s, 'utf8').digest('hex');
}

function activationCode(deviceCode) {
    return sha256Hex(SECRET + '-' + deviceCode).substring(0, 8).toUpperCase();
}

function sendJson(res, obj, status = 200) {
    const body = JSON.stringify(obj);
    res.writeHead(status, {
        'Content-Type': 'application/json; charset=utf-8',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    });
    res.end(body);
}

function sendHtml(res, html) {
    res.writeHead(200, {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-store'
    });
    res.end(html);
}

function isAdmin(req, url) {
    const config = loadConfig();
    if (!config.adminToken) return false;
    const token = url.searchParams.get('token') || '';
    if (token === config.adminToken) return true;
    const auth = req.headers.authorization || '';
    return auth === 'Bearer ' + config.adminToken;
}

function markUnlocked(device) {
    if (!device) return false;
    device = String(device).toUpperCase();
    const unlocks = loadUnlocks();
    unlocks[device] = true;
    saveUnlocks(unlocks);
    // 已解锁的设备从待确认队列里清掉
    const pending = loadPending();
    if (pending[device]) {
        delete pending[device];
        savePending(pending);
    }
    return true;
}

function serveApk(req, res, filename) {
    const safe = path.basename(filename).replace(/[^a-zA-Z0-9._-]/g, '_');
    const filePath = path.join(DOWNLOAD_DIR, safe);
    if (!fs.existsSync(filePath)) {
        return sendJson(res, { ok: false, error: 'file_not_found' }, 404);
    }

    if (req.method === 'GET') {
        const data = loadDownloads();
        recordDownload(data, safe, todayStr());
        saveDownloads(data);
    }

    const stat = fs.statSync(filePath);
    res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="' + safe + '"',
        'Cache-Control': 'no-store'
    });
    if (req.method === 'HEAD') {
        return res.end();
    }
    fs.createReadStream(filePath).pipe(res);
}

function renderStatsHtml(data) {
    const stats = downloadsPayload(data);
    let rows = '';
    for (const name of Object.keys(stats.files)) {
        const f = stats.files[name];
        rows += `<tr><td>${name}</td><td>${f.total}</td><td>${f.today}</td><td>${f.last7}</td></tr>`;
    }
    if (!rows) rows = '<tr><td colspan="4">暂无下载记录</td></tr>';

    let historyRows = '';
    const days = Object.keys(stats.history).sort().reverse();
    for (const day of days.slice(0, 30)) {
        historyRows += `<tr><td>${day}</td><td>${stats.history[day]}</td></tr>`;
    }
    if (!historyRows) historyRows = '<tr><td colspan="2">暂无历史记录</td></tr>';

    return `<!doctype html><html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
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
</body></html>`;
}

const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    const pathname = url.pathname;

    if (req.method === 'OPTIONS') {
        return sendJson(res, { ok: true });
    }

    if (pathname === '/health') {
        return sendJson(res, { ok: true });
    }

    // 下载统计：/api/qingkebiao/download/<文件名>
    const dlMatch = pathname.match(/^\/api\/qingkebiao\/download\/(.+)$/);
    if (dlMatch && (req.method === 'GET' || req.method === 'HEAD')) {
        let fileName;
        try {
            fileName = decodeURIComponent(dlMatch[1]);
        } catch (e) {
            fileName = dlMatch[1];
        }
        return serveApk(req, res, fileName);
    }

    // 仅管理员可查看的统计 JSON
    if (pathname === '/api/qingkebiao/downloads') {
        if (!isAdmin(req, url)) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        return sendJson(res, downloadsPayload(loadDownloads()));
    }

    // 仅管理员可查看的统计 HTML
    if (pathname === '/api/qingkebiao/downloads.html') {
        if (!isAdmin(req, url)) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        return sendHtml(res, renderStatsHtml(loadDownloads()));
    }

    // App 查询当前设备是否已解锁
    if (pathname === '/api/qingkebiao/unlock' && req.method === 'GET') {
        const device = (url.searchParams.get('device') || '').toUpperCase();
        const unlocks = loadUnlocks();
        return sendJson(res, { unlocked: unlocks[device] === true });
    }

    // App 输入激活码/凭证在线验证（离线激活码也能在这里确认）
    if (pathname === '/api/qingkebiao/unlock' && req.method === 'POST') {
        let raw = '';
        req.on('data', chunk => { raw += chunk; if (raw.length > 1024 * 1024) req.destroy(); });
        req.on('end', () => {
            try {
                const body = JSON.parse(raw || '{}');
                const device = String(body.device || '').toUpperCase();
                const proof = String(body.proof || '').trim().toUpperCase();
                const unlocks = loadUnlocks();

                let unlocked = unlocks[device] === true;
                if (!unlocked && proof && proof === activationCode(device)) {
                    // 走统一入口：顺便把该设备从待确认队列里清掉
                    markUnlocked(device);
                    unlocked = true;
                }
                sendJson(res, { unlocked });
            } catch (e) {
                sendJson(res, { unlocked: false, error: 'bad_request' }, 400);
            }
        });
        return;
    }

    // 用户在 App 里点「我已赞赏」后登记设备码（公开接口，只需设备码格式合法）
    if (pathname === '/api/qingkebiao/pending' && req.method === 'POST') {
        let raw = '';
        req.on('data', chunk => { raw += chunk; if (raw.length > 64 * 1024) req.destroy(); });
        req.on('end', () => {
            try {
                const body = JSON.parse(raw || '{}');
                const device = String(body.device || '').trim().toUpperCase();
                if (!deviceCodeValid(device)) {
                    return sendJson(res, { ok: false, error: 'invalid_device' }, 400);
                }
                const pending = loadPending();
                // 队列上限：满了直接拒绝新登记，不再淘汰最旧的
                // （否则有人刷假设备码就能把真实待确认用户挤掉）
                if (!pending[device] && Object.keys(pending).length >= 1000) {
                    return sendJson(res, { ok: false, error: 'queue_full' }, 429);
                }
                pending[device] = { t: new Date().toISOString() };
                savePending(pending);
                // 如果其实已经解锁过，直接返回解锁状态
                const unlocks = loadUnlocks();
                sendJson(res, { ok: true, unlocked: unlocks[device] === true });
            } catch (e) {
                sendJson(res, { ok: false, error: 'bad_request' }, 400);
            }
        });
        return;
    }

    // 管理员查看待确认队列（JSON）
    if (pathname === '/api/qingkebiao/pending' && req.method === 'GET') {
        if (!isAdmin(req, url)) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        const devices = loadPending();
        const list = Object.keys(devices)
            .map(code => ({ device: code, ...devices[code] }))
            .sort((a, b) => (a.t || '') > (b.t || '') ? -1 : 1);
        return sendJson(res, { count: list.length, devices: list });
    }

    // 管理员从待确认队列里删掉某条（刷进来的垃圾登记）
    if (pathname === '/api/qingkebiao/pending/remove' && req.method === 'POST') {
        const config = loadConfig();
        const token = url.searchParams.get('token') || '';
        const device = (url.searchParams.get('device') || '').toUpperCase();
        if (!config.adminToken || token !== config.adminToken) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        const pending = loadPending();
        if (device) {
            delete pending[device];
        }
        savePending(pending);
        return sendJson(res, { ok: true, removed: device || null });
    }

    // 管理员清空整个待确认队列
    if (pathname === '/api/qingkebiao/pending/clear' && req.method === 'POST') {
        const config = loadConfig();
        const token = url.searchParams.get('token') || '';
        if (!config.adminToken || token !== config.adminToken) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        savePending({});
        return sendJson(res, { ok: true, cleared: true });
    }

    // 管理员确认某个设备码已赞赏 -> 标记解锁（也可用于手动标记任意设备）
    if (pathname === '/api/qingkebiao/confirm' && req.method === 'POST') {
        const config = loadConfig();
        const token = url.searchParams.get('token') || '';
        const device = (url.searchParams.get('device') || '').toUpperCase();
        if (!config.adminToken || token !== config.adminToken || !deviceCodeValid(device)) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        markUnlocked(device);
        return sendJson(res, { ok: true, unlocked: true, device });
    }

    // 管理员赞赏确认页：列出待确认设备码，核对赞赏记录后一键确认
    if (pathname === '/api/qingkebiao/pending.html') {
        if (!isAdmin(req, url)) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        const devices = loadPending();
        const list = Object.keys(devices)
            .map(code => ({ device: code, ...devices[code] }))
            .sort((a, b) => (a.t || '') > (b.t || '') ? -1 : 1);
        let rows = '';
        for (const item of list) {
            const time = item.t ? new Date(item.t).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '未知';
            rows += `<tr><td><b>${item.device}</b></td><td>${time}</td>
<td><button onclick="confirmDevice('${item.device}')">确认赞赏</button>
<button onclick="copyCode('${item.device}')" style="margin-left:8px">复制码</button>
<button onclick="removeDevice('${item.device}')" style="margin-left:8px">删除</button></td></tr>`;
        }
        if (!rows) rows = '<tr><td colspan="3">暂无待确认设备</td></tr>';

        const html = `<!doctype html><html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>LiteSchedule 赞赏确认</title>
<body style="font-family: sans-serif; padding: 24px; background:#f4f6fb; color:#2d3142">
<h2>LiteSchedule 赞赏确认</h2>
<p>用户在 App 里赞赏后点「我已赞赏」，设备码会进入下面队列。对照微信「赞赏记录」（昵称/金额/时间）确认后点「确认赞赏」，用户 App 会在 30 秒内自动解锁。</p>
<table border="1" cellpadding="6" style="border-collapse:collapse;background:#fff">
<tr><th>设备码</th><th>登记时间</th><th>操作</th></tr>
${rows}
</table>
<p><button onclick="clearQueue()">清空待确认队列</button>（只清登记，不影响已解锁设备）</p>
<h3>手动标记</h3>
<p>不在队列里的设备码也可以直接标记（比如用户通过其它渠道联系你）：</p>
<form onsubmit="manualMark(); return false;">
<input id="manual_device" placeholder="8 位设备码，如 1A2B3C4D" style="width:220px;padding:6px;margin-right:8px">
<button>标记解锁</button>
<span id="manual_msg" style="margin-left:12px;color:#2563eb"></span>
</form>
<script>
async function confirmDevice(code) {
    if (!confirm('确认设备 ' + code + ' 已赞赏？')) return;
    const r = await fetch('/api/qingkebiao/confirm?token=${encodeURIComponent(url.searchParams.get('token') || '')}&device=' + code, { method: 'POST' });
    const j = await r.json();
    if (j.ok) { location.reload(); } else { alert('确认失败：' + (j.error || '未知错误')); }
}
async function removeDevice(code) {
    if (!confirm('从待确认队列删除 ' + code + '？（不影响已解锁状态）')) return;
    const r = await fetch('/api/qingkebiao/pending/remove?token=${encodeURIComponent(url.searchParams.get('token') || '')}&device=' + code, { method: 'POST' });
    const j = await r.json();
    if (j.ok) { location.reload(); } else { alert('删除失败：' + (j.error || '未知错误')); }
}
async function clearQueue() {
    if (!confirm('清空整个待确认队列？')) return;
    const r = await fetch('/api/qingkebiao/pending/clear?token=${encodeURIComponent(url.searchParams.get('token') || '')}', { method: 'POST' });
    const j = await r.json();
    if (j.ok) { location.reload(); } else { alert('清空失败：' + (j.error || '未知错误')); }
}
function copyCode(code) {
    navigator.clipboard && navigator.clipboard.writeText(code);
    alert('已复制 ' + code);
}
async function manualMark() {
    const code = document.getElementById('manual_device').value.trim().toUpperCase();
    const msg = document.getElementById('manual_msg');
    if (!/^[A-F0-9]{8}$/.test(code)) { msg.textContent = '设备码格式不对'; return; }
    const r = await fetch('/api/qingkebiao/confirm?token=${encodeURIComponent(url.searchParams.get('token') || '')}&device=' + code, { method: 'POST' });
    const j = await r.json();
    msg.textContent = j.ok ? '已标记解锁 ' + code : '失败：' + (j.error || '未知错误');
    if (j.ok) location.reload();
}
</script>
</body></html>`;
        return sendHtml(res, html);
    }

    // 管理员手动标记
    if (pathname === '/api/qingkebiao/mark' && req.method === 'POST') {
        const config = loadConfig();
        const token = url.searchParams.get('token') || '';
        const device = (url.searchParams.get('device') || '').toUpperCase();
        if (!config.adminToken || token !== config.adminToken || !device) {
            return sendJson(res, { ok: false, error: 'forbidden' }, 403);
        }
        markUnlocked(device);
        return sendJson(res, { ok: true, unlocked: true, device });
    }

    // 支付回调预留：等接入微信/支付宝/易支付后，由支付平台把这里改成对应入参和验签
    if (pathname === '/api/qingkebiao/pay/notify' && req.method === 'POST') {
        let raw = '';
        req.on('data', chunk => { raw += chunk; if (raw.length > 1024 * 1024) req.destroy(); });
        req.on('end', () => {
            try {
                const body = JSON.parse(raw || '{}');
                const config = loadConfig();
                const device = String(body.device || '').toUpperCase();
                const status = String(body.status || '').toLowerCase();
                const sign = String(body.sign || '').toLowerCase();
                // 简单约定签名：HMAC-SHA256(paySecret, device + "|" + status)
                const expect = hmacHex(config.paySecret || '', device + '|' + status).toLowerCase();
                if (!device || !config.paySecret || sign !== expect) {
                    return sendJson(res, { ok: false, error: 'invalid_sign' }, 403);
                }
                if (status === 'success') {
                    markUnlocked(device);
                    return sendJson(res, { ok: true, unlocked: true });
                }
                return sendJson(res, { ok: true, unlocked: false });
            } catch (e) {
                sendJson(res, { ok: false, error: 'bad_request' }, 400);
            }
        });
        return;
    }

    sendJson(res, { ok: false, error: 'not_found' }, 404);
});

server.listen(PORT, '127.0.0.1', () => {
    console.log('qk unlock server listening on ' + PORT);
});
