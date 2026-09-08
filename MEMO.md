# LiteSchedule Memo

## 当前状态

- App 名：`LiteSchedule`
- 版本：`1.0.3`（versionCode 4，`/home/hoshi/Kebiao/qingkebiao_v1.0.3.apk`，已装到测试机）
- GitHub：https://github.com/HoshiGT/LiteSchedule-Android
- 赞赏/在线解锁：https://schedule.hoshichan.moe
- 测试机无线 ADB：`192.168.8.107:38559`（端口已从 40877 改为 38559）

## 核心功能

- 周课表网格（ViewPager2 翻页）
- 当前周次切换 + 左右滑动吸附
- 按课程周次过滤（例：1-8 周 A，9-16 周 B）
- 自定义开学日期 / 自定义上课时间
- 点击课表空白处快速添加课程（气泡 + 拖钮）
- 教务导入：自定义学校网址、导出 XLS / XML 自动识别导入
- 今日课程桌面小组件
- 上课提醒（AlarmManager + 通知）
- 自定义背景（预设 + 相册图片）
- 10/50/100 天赞赏提醒
- 在线解锁 / 赞赏页：https://schedule.hoshichan.moe
- 服务器下载统计（仅管理员可查看）：`server/server.js` / `server/download-stats.js`
- 开源仓库 / 赞赏入口已分离

## v1.0.3 新增：赞赏判定（登记-确认）流程

不再靠「聊天发设备码 + 算激活码」，改为：

1. 用户扫赞赏码付款后，在 App 赞赏页点「我已赞赏，提交登记」
   - App 调 `POST /api/qingkebiao/pending`（body `{"device":"设备码"}`，公开接口、校验 8 位 HEX）
2. 作者在管理页核对微信赞赏记录（昵称/金额/时间），一键确认
   - `https://schedule.hoshichan.moe/api/qingkebiao/pending.html?token=<adminToken>`
   - 确认接口：`POST /api/qingkebiao/confirm?token=<adminToken>&device=<设备码>`
   - 队列查询（JSON）：`GET /api/qingkebiao/pending?token=<adminToken>`
3. App 在赞赏页每 30 秒轮询 `GET /api/qingkebiao/unlock?device=`，
   作者确认后自动解锁，用户无需再输激活码。
- 数据：`/opt/qk-server/pending.json`（队列上限 1000，自动淘汰最旧）
- 离线激活码/凭证输入框保留作备用（断网或服务器故障时）
- 已本地冒烟测试通过：登记、非法设备码拒绝、队列查询鉴权、确认解锁、队列清空、手动标记、错误 token 403
- `markUnlocked` 现在会顺带把该设备从待确认队列移除

## 构建

```bash
cd /home/hoshi/Kebiao/qk_java
# 详细构建命令见 HANDOFF.md（aapt2 compile/link -> javac -> d8 -> zip dex -> apksigner）
# v1.0.3 已用 versionCode 4 / versionName 1.0.3 构建并签名（证书同前，可覆盖安装）
```

## 明天待办（重要）

1. **修 VPS SSH**：`cancon.hpccube.com:65023`（kunshan 配置）publickey 认证被拒，
   两把 key（kunshan_acx0ui86yf / kunshan_alt）都不认了，需要重新加公钥或换 key
2. **部署服务端**：
   ```bash
   scp /home/hoshi/Kebiao/server/server.js <user>@cancon.hpccube.com:/opt/qk-server/server.js
   ssh kunshan 'systemctl restart qk-unlock'
   ```
   部署前新接口是 404（App 点「我已赞赏」会提示登记失败）
3. **部署新 APK**：
   ```bash
   cp /home/hoshi/Kebiao/qingkebiao_v1.0.3.apk /var/www/schedule.hoshichan.moe/html/LiteSchedule-v1.0.3.apk
   ```
   （nginx 已有 `~ ^/(LiteSchedule-.*\.apk)$` 规则自动 302 到统计入口，无需改配置）
4. **真机全链路测试**：
   - `adb -s 192.168.8.107:38559 shell pm clear com.hoshi.qingkebiao` 重置解锁状态
   - 赞赏页点「我已赞赏」→ 管理页确认 → 观察 App 30 秒内自动解锁
5. （可选，彻底免人工）接聚合支付平台（虎皮椒/易支付，个人收款码即可，费率约 1-2%）：
   用户点按钮跳转支付页，平台带签名回调 `POST /api/qingkebiao/pay/notify`，服务端验签后自动解锁。
   server.js 里已留该端点（当前按 HMAC 约定实现，接平台时改成对应验签）

## 已知问题 / 后续

- 教务 WebView 的顶部 logo 栏在不同 UA 下显示仍有差异；当前提供“电脑版”勾选框切换
- 桌面小组件目前是列表式今日课程；更复杂的“桌面直接拖拽添加课程”需要悬浮窗方案
- 微信「赞赏码」本身没有任何 API/回调，付款方匿名，所以无法全自动判定；
  当前方案是「用户登记 + 作者一键确认」，人工成本从「聊天+算码+回发+用户再输入」降为「点一下」
- 新的问题建议新开窗口，携带本文件继续

- 国内镜像下载：https://schedule.hoshichan.moe/LiteSchedule-v1.0.3.apk（部署后生效）
