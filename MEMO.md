# LiteSchedule Memo

## 当前状态

- App 名：`LiteSchedule`
- 版本：`1.0.3`（versionCode 4，`/home/hoshi/Kebiao/qingkebiao_v1.0.3.apk`，已装到测试机）
- GitHub：https://github.com/HoshiGT/LiteSchedule-Android
- 赞赏/在线解锁：https://schedule.hoshichan.moe
- 测试机无线 ADB：`192.168.8.107:38559`（端口已从 40877 改为 38559）
  - ⚠️ 验收时误执行 `svc wifi disable`，手机 WiFi 被关、ADB 断开，需手动重开 WiFi 后再 `adb connect`
  - 赞赏页解锁状态存在 `shared_prefs/qingkebiao.xml` 的 `background_unlocked`；
    但每次打开赞赏页都会联网复查，服务端说 true 会把它改回 true

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

1. **服务器 SSH 已确认可用**（2026-09-08 验收时实测）：
   - 真实服务器是 `155.103.157.120`（工作区 HANDOFF.md 第 189 行，未提交、勿写进公开仓库）
   - `ssh root@155.103.157.120`（默认密钥 `~/.ssh/id_ed25519`）可登录
   - ⚠️ `cancon.hpccube.com:65023`（ssh config 里的 `kunshan`）是**昆山超算集群，不是本项目服务器**，之前记的"VPS SSH 挂了"是判错主机
   - ⚠️ 连续多次失败登录会触发服务器 fail2ban，本机 IP 被临时拒连（22 端口 Connection refused），等 10 分钟或让服务器侧解封
2. **部署服务端**：
   ```bash
   scp server/server.js root@155.103.157.120:/opt/qk-server/server.js
   ssh root@155.103.157.120 'systemctl restart qk-unlock && systemctl is-active qk-unlock'
   ```
   部署前新接口是 404（App 点「我已赞赏」会提示登记失败，已验证不会崩）
3. **部署新 APK**：
   ```bash
   cp /home/hoshi/Kebiao/qingkebiao_v1.0.3.apk /var/www/schedule.hoshichan.moe/html/LiteSchedule-v1.0.3.apk
   ```
   （nginx 已有 `~ ^/(LiteSchedule-.*\.apk)$` 规则自动 302 到统计入口，无需改配置）
4. **生产端到端复验**（本地已跑通，这步只是换成生产地址再确认一遍）：
   - 手机设备码**必须从 App 界面读**（赞赏页显示的那个，本次是 `C9B56FE0`），
     ⚠️ 不要用 `adb shell settings get secure android_id` 去算，Android 8+ 两者不同
   - 该设备码当前在生产 `unlocks.json` 里是 `true`，要验"未解锁→登记→确认"得先把它删掉
   - 流程：赞赏页点「我已赞赏」→ 查 `/api/qingkebiao/pending?token=` 是否收到 → 管理页点「确认赞赏」
     → 观察 App 在 30 秒内自动解锁（prefs 里 `background_unlocked` 变 true、UI 切到已解锁面板）
   - 现场恢复：把设备码重新标记解锁（或走确认流程），装回生产 APK
5. **（建议）把今天这套复测固化成 `server/verify.sh`**：一条命令跑完
   顺路径 + 异常路径 + 鉴权 + 队列清理 + 老功能回归，退出码说话，验收不再靠模型自述
6. （可选，彻底免人工）接聚合支付平台（虎皮椒/易支付，个人收款码即可，费率约 1-2%）：
   用户点按钮跳转支付页，平台带签名回调 `POST /api/qingkebiao/pay/notify`，服务端验签后自动解锁。
   server.js 里已留该端点（当前按 HMAC 约定实现，接平台时改成对应验签）

## 端到端验收结果（2026-09-08，真机 + 本地服务端）

用「临时指向本机服务器的测试包」在 OnePlus 8T 上跑通了完整链路（不依赖生产部署）：

1. 测试包（`http://192.168.8.247:8799`）安装后处于锁定态，界面正确显示：
   设备码 `C9B56FE0`、「我已赞赏，提交登记」按钮、备用激活码输入框
2. 点按钮 → 本机服务端 `pending.json` 立刻收到 `C9B56FE0`（App→服务端登记链路 ✓）
3. 管理接口确认 → `{"ok":true,"unlocked":true}`，队列自动清空 ✓
4. **App 在 35 秒内自动解锁**（30s 轮询生效），UI 切换到「已解锁，可切换预设背景或导入本地图片」，
   全程用户没有再输任何激活码 ✓
5. 断网状态点按钮 → 无崩溃、按钮自动恢复可点 ✓
6. 现场已还原：装回生产版 APK（资源里确认是 `https://schedule.hoshichan.moe`）、
   iptables 规则已删、App 数据（解锁状态/教务网址）保留

关键经验：**Android 8+ 的 ANDROID_ID 按「签名+用户」隔离**，
用 `adb shell settings get secure android_id` 算出来的设备码和 App 内的**不是同一个**，
验证时要直接从 App 界面读设备码。

## 小组件「有课却显示没课」bug（2026-09-14 已修，v1.0.4）

**现象**：跨到新的一周后，今日课程小组件一直显示「今日无课 / 明日无课」；
日期、周次、星期都正确，点箭头切明天再切回来也还是空。

**根因**：小组件的两部分数据走的是**两条独立的更新链路**——
- 日期/周次/箭头：`updateAll()` 里直接 `setTextViewText`，静态 RemoteViews，随 `updateAppWidget` 立即生效；
- 课程列表：`ListView` + `RemoteViewsService`，要等 Launcher 去**绑定服务**取数。

App 被系统冻结/回收时（ColorOS 的 `OplusHansManager` 日志能看到 `F enter()`、`unproxyAlarmsByUid`），
服务绑不上，集合视图就一直是空的，而头部照样是新的 → 「日期周次都对，就是没课」。

**修法**（改 `TodayWidgetProvider.java` + `widget_today.xml`，删掉 `TodayWidgetRemoteViewsService`）：
1. 课程列表改成**静态 RemoteViews 直出**：布局预置 6 行，更新时按需 `setViewVisibility` + 填文本，
   与头部在**同一次 updateAppWidget** 里下发 → 不可能再不一致，也不依赖绑定服务
   （实测：`am kill` 杀掉 App 进程后小组件照常显示课程）
2. 显示几行**按小组件实际高度自适应**（`capacityFor()` 读 `OPTION_APPWIDGET_MIN_HEIGHT`），
   装不下就少显示几门 + 最后一行「还有 N 门课…」，不会再被裁
3. 行高保持旧版观感（10dp 内边距、12sp/11sp、每行 109px）
4. 顺带修掉潜在崩溃：`Math.abs(name.hashCode())` 在 hashCode 为 `Integer.MIN_VALUE` 时仍为负 → 数组负下标越界
5. 跨天（DATE_CHANGED / 每日闹钟）时把「预览明天」的 offset 复位，避免新的一天还停在昨天选的偏移上
6. 午夜刷新改用 `setExactAndAllowWhileIdle`（有精确闹钟权限时），doze 下更准点

**真机时间穿越复现测试（8T / v1.0.4）**：
先 `settings put global auto_time 0`，用 `su -c date -s "YYYY-MM-DD 12:00:00"` 改时间，测完恢复 `auto_time 1`；
按「周六 → 周日 → 周一」顺序，每次点小组件箭头手动刷新：

| 时间 | 小组件显示 |
|---|---|
| 周六 09-19 | `2026/09/20 第3周 周日 · 明日无课` ✓（周末确实无课） |
| 周日 09-20 | `2026/09/20 第3周 周日 · 今日无课` ✓ |
| **周一 09-21（跨进第4周）** | **`第4周 周一 · 计算机图形学 / 印刷化学与材料 / 跨媒体信息技术`** ✓ **原 bug 未复现** |

已知小限制：把系统时间**往回调**时小组件不会自动刷新（往回调不会触发已排定的闹钟，
ColorOS 还会吞掉系统广播），点一下箭头或等下一次更新即可；正常使用遇不到。

**荣耀（HONOR LSA-AN00 / Android 14，无 root）实机确认**：
- 装 v1.0.2 时复现了同一 bug：`2026/09/14 第3周 周一` + `今日无课`，
  而 App 内明明写着「第3周 · 本周课程（12门）」→ 确诊是小组件渲染链路问题，不是数据空
- 装 v1.0.4 后：`第3周 周一` + **计算机图形学 / 印刷化学与材料 / 跨媒体信息技术** 三门课全部正常显示
- ⚠️ **荣耀启动器只上报默认最小高度 110dp，不随实际尺寸更新**（实际小组件高 554px ≈ 185dp），
  照上报值算会得出「只够 1 行」→ 已加兜底常量 `MIN_TRUSTED_HEIGHT_DP = 180`
  （低于此值一律按 180dp 算，即头部 60dp + 3 行课程）
- 无 root 的机器不能 `date -s` 改时间，验证跨周只能靠真机等时间到点或换有 root 的机器

验证技巧：shell(uid 2000) 不能发 `DATE_CHANGED` 这种受保护广播，
要模拟"午夜刷新"用自定义 action：`am broadcast -a com.hoshi.qingkebiao.WIDGET_DAY_TICK -n com.hoshi.qingkebiao/.TodayWidgetProvider`

## 验收发现（2026-09-08 复测，待修）

1. ~~`POST /unlock` 用离线激活码自解锁时不清 pending 队列~~ ✅ 已修（改走 `markUnlocked`），复测通过
2. ~~`POST /pending` 队列满了淘汰最旧（可被刷假设备码挤掉真实用户）~~ ✅ 已修
   （满了返回 429 `queue_full`，已登记设备仍可刷新时间），并新增管理页「删除」「清空队列」
3. **管理页 token 走 URL query**：会进 nginx access log / 浏览器历史；
   若要更稳，改成 POST + Authorization 头
4. **离线激活码 secret 在公开仓库里**（`UnlockManager.java` + `server.js` 都是公开文件），
   任何人可自算激活码绕过赞赏；新登记-确认流程不依赖该 secret，后续可考虑只留联网验证
5. 轮询只在赞赏页前台且未解锁时进行；用户登记后若离开该页，要等下次打开才会解锁（可接受，但要知道）
6. `POST /pending` 仍无频率限制（只挡了队列满）；要更稳可按 IP 限流

## 本地模型（Qwen3 Q3_K_M + Q4 KV / 128K）的已知退化点

本次验收看到的失败模式都指向"长程召回差 + 过早下结论"，与量化策略吻合：

- Q3_K_M 对**自省/自我质疑**能力影响最大 → 它不会主动问"还有哪条路径没测"
- KV 的 **K 比 V 敏感**，K/V 都压 Q4 时，长上下文里"定位到某个具体 token"的能力下降最快
  → 远端细节（HANDOFF 里的 IP、之前测过的路径）容易丢
- 建议：`-ctk q8_0 -ctv q4_0`（或 K 用 f16）；上下文 32–48K + 文件记忆 + grep 取数，
  通常好过 128K 全塞；事实性断言要求带 `文件:行号`，禁止无证据的"全过/挂了"

## 已知问题 / 后续

- 教务 WebView 的顶部 logo 栏在不同 UA 下显示仍有差异；当前提供“电脑版”勾选框切换
- 桌面小组件目前是列表式今日课程；更复杂的“桌面直接拖拽添加课程”需要悬浮窗方案
- 微信「赞赏码」本身没有任何 API/回调，付款方匿名，所以无法全自动判定；
  当前方案是「用户登记 + 作者一键确认」，人工成本从「聊天+算码+回发+用户再输入」降为「点一下」
- 新的问题建议新开窗口，携带本文件继续

- 国内镜像下载：https://schedule.hoshichan.moe/LiteSchedule-v1.0.3.apk（部署后生效）
