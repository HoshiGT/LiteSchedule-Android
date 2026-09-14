# LiteSchedule 下载统计

已集成到现有 `/opt/qk-server/server.js`，不需要额外端口。

## 已部署内容

- `/opt/qk-server/server.js`
  - 新增下载统计接口
  - 保留原有解锁/激活码/支付回调功能
- `/opt/qk-server/downloads.json`
  - 统计数据的存储文件（首次请求后自动生成）
- Nginx（`schedule.hoshichan.moe`）
  - 旧的直链 APK 会自动 302 到统计接口
  - `/api/qingkebiao/` 继续反代到本机 `8765`

## 接口

### 统计查看（仅管理员，带 token）

```text
https://schedule.hoshichan.moe/api/qingkebiao/downloads?token=你的adminToken
https://schedule.hoshichan.moe/api/qingkebiao/downloads.html?token=你的adminToken
```

- `downloads`：JSON 统计
- `downloads.html`：简单的浏览器统计页
- 不带正确 token：`403`

### 下载统计入口

```text
https://schedule.hoshichan.moe/api/qingkebiao/download/LiteSchedule-v1.0.2.apk
```

也可以继续用原来的直链：

```text
https://schedule.hoshichan.moe/LiteSchedule-v1.0.2.apk
```

Nginx 会把直链 302 到上面的统计入口，所以两种情况都会计数。

## 数据格式

`/opt/qk-server/downloads.json`：

```json
{
  "files": {
    "LiteSchedule-v1.0.2.apk": {
      "total": 1,
      "daily": {
        "2026-09-01": 1
      }
    }
  },
  "history": {
    "2026-09-01": 1
  },
  "lastUpdated": "2026-09-01T11:01:38.542Z"
}
```

## 本仓库文件

- `server.js`：整合后的完整服务端代码（已部署到服务器）
- `schedule.hoshichan.moe.nginx`：对应 Nginx 配置
- `download-stats.js`：独立版下载统计服务（如果以后不想改原 server.js，可以单独跑这个）
