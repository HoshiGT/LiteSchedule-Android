# LiteSchedule Memo

## 当前状态

- App 名：`LiteSchedule`
- 版本：**`1.0.5`**（versionCode 6，`qingkebiao_v1.0.5.apk`，两台真机都已装）
- GitHub：https://github.com/HoshiGT/LiteSchedule-Android
- Release：**v1.0.5**（tag + release 已推，APK 作为 release 附件，1046164 字节 / md5 `cb77bcd6`）
  https://github.com/HoshiGT/LiteSchedule-Android/releases/tag/v1.0.5
- 镜像下载：https://schedule.hoshichan.moe/LiteSchedule-v1.0.5.apk （站点首页也已指向 v1.0.5，下载统计已计数）
- 赞赏/在线解锁服务：`155.103.157.120:/opt/qk-server/server.js`，**已部署新版**（含 pending 接口）
- 真机：
  - OnePlus 8T `192.168.8.107:38283`（有 root，可 `date -s` 做时间穿越）
  - 荣耀 LSA-AN00 `192.168.8.201:37859`（无 root）
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

## v1.0.3 新增、v1.0.4 发布：赞赏判定（登记-确认）流程

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
cd qk_java
./build.sh                                  # 默认 5 / 1.0.4，输出 ../qingkebiao.apk
VERSION_CODE=6 VERSION_NAME=1.0.5 OUT=../qingkebiao_v1.0.5.apk ./build.sh
```

⚠️ **构建两个坑（2026-09-14 踩过）**：
1. `gen/androidx.{core,customview,recyclerview,viewpager2}/R.java` 是**预生成入库**的
   （之前被 gitignore，导致"干净克隆"编出来的 APK 缺少 AndroidX 的 R 类，装上会崩）。
   已改：`.gitignore` 只忽略 `qk_java/gen/com/`，AndroidX 那四个 R.java 已入库。
   构建时**不要 `rm -rf gen/`**，`build.sh` 只重建 `gen/com`。
2. `res/drawable/mm_reward_qrcode.png` 在 index 里被标了 **skip-worktree**：
   仓库内是脱敏版（300×300），本地构建用完整版（943×943）。
   所以干净克隆构建出的 APK 只在这张图上不同（体积差约 118KB），**代码部分 dex md5 完全一致**。

## 待办

1. ~~部署服务端~~ ✅ 2026-09-14 已部署（`/opt/qk-server/server.js` 含 pending 接口，服务 active，
   外网 `POST /api/qingkebiao/pending` 返回 `{"ok":true}`；旧版已备份为 `server.js.bak.20260914053020`）
2. ~~部署 APK 到镜像站~~ ✅ 已传 `LiteSchedule-v1.0.4.apk`，站点首页也改成指向 v1.0.4，
   下载链路（nginx → 统计接口）与下载计数均已验证
3. **生产端到端复验**（登记→确认→自动解锁）：本地已跑通，生产接口也已上线，
   但**还没用真机在生产环境完整走一遍**。设备码要从 App 界面读（8T 是 `C9B56FE0`），
   ⚠️ 不要用 `adb shell settings get secure android_id` 算，Android 8+ 两者不同。
   该设备码在生产 `unlocks.json` 里已是 `true`，要验"未解锁→登记→确认"得先删掉它。
4. **（建议）把验收复测固化成 `server/verify.sh`**：一条命令跑完
   顺路径 + 异常路径 + 鉴权 + 队列清理 + 老功能回归，退出码说话，验收不再靠模型自述
5. （可选，彻底免人工）接聚合支付平台（虎皮椒/易支付，个人收款码即可，费率约 1-2%）：
   用户点按钮跳转支付页，平台带签名回调 `POST /api/qingkebiao/pay/notify`，服务端验签后自动解锁。
   server.js 里已留该端点（当前按 HMAC 约定实现，接平台时改成对应验签）
6. 服务器运维备忘：
   - `ssh root@155.103.157.120`（默认密钥）可登录；`cancon.hpccube.com:65023`（ssh config 里的 `kunshan`）
     是**昆山超算集群、不是本项目服务器**
   - 连续多次失败登录会触发 fail2ban，本机 IP 被临时拒连（22 端口 Connection refused）

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

## 小组件改成可滑动列表（2026-09-15，v1.0.5）

**需求**：满课（4 门以上）时不要显示「还有 N 门课…」，要能**滑动看全**。

**做法**：Android 12（API 31）提供了 `RemoteViews.setRemoteAdapter(viewId, RemoteCollectionItems)`，
可以把列表项**直接塞进 RemoteViews**——既能滑动，又**不需要绑定 RemoteViewsService**
（正好绕开「App 被冻结 → 服务绑不上 → 列表空」那个老坑）。注意这个重载**不带 appWidgetId**。

- API ≥ 31：`widget_list`（ListView）+ `RemoteCollectionItems`（每项一个 `widget_course_item` RemoteViews）
- API < 31：退回静态行方案（`widget_items` + 空格占位 + 「还有 N 门课…」）

**顺带修掉的交互 bug**：原来「打开 App」的点击挂在**根布局**上，
导致在列表上滑动时启动器会当成"按下了小组件"——播放**点击缩小动效**、还抢滚动手势。
改成只把点击挂在**头部**（`widget_header`）和「今日无课」提示上，列表区域就干净了。

**验证矩阵（8T，`date -s` 穿越 + `WIDGET_DAY_TICK` 广播刷新）**：

| 场景 | 结果 |
|---|---|
| 周二 3 门 | 列表 3 项 ✓ |
| **周四 4 门（满课）** | 3 项可见 + **列表内滑动划出第 4 门** ✓（滑动不再翻页/开抽屉） |
| 周六 0 门 | 「今日无课」，列表收起 ✓ |
| `am kill` 杀掉 App 进程 | 列表照常显示 ✓（静态下发，不怕冻结） |
| 点击头部 | 打开 App ✓（`mCurrentFocus=…MainActivity`） |

## 小组件最后一行被裁（2026-09-15 已修，v1.0.5）

**现象**：小组件最下面那节课显示不全，第三节课的文字被截断一半。

**根因**：课程行原来是 `wrap_content` + 固定内边距（自然高度 113px），
而 `LinearLayout` **不会压缩** `wrap_content` 的子项——容器只有 352px，塞 3 行需要 375px，
最后一行被按剩余空间重新测量，从 113px 压到 90px，文字从 53px 裁到 30px。
（荣耀 LSA-AN00 实测：行高 113/113/**90**，文字 53/53/**30**）

**修法**：课程行改成**等分权重**（`layout_height=0dp` + `layout_weight=1`），
可用高度由所有可见子项平分，最后一行不再被单独压缩；
内边距 10dp → 8dp（保证压缩后文字仍放得下）；
再放 5 个「空格占位」（同样权重、默认 GONE），课少时按 `cap - 可见行数` 显示，
避免只有一门课时那行被撑成一个大泡泡。

**实测对比（荣耀）**：
| | 修复前 v1.0.4 | 修复后 v1.0.5 |
|---|---|---|
| 行1 | 113px / 文字 53px ✓ | 105px / 文字 53px ✓ |
| 行2 | 113px / 文字 53px ✓ | 105px / 文字 53px ✓ |
| 行3 | **90px / 文字 30px ✗** | **106px / 文字 53px ✓** |

**边界场景验证矩阵（8T 有 root，用 `date -s` 穿越 + `am broadcast ... WIDGET_DAY_TICK` 强制刷新）**：

| 场景 | 结果 |
|---|---|
| 周二 3 门（8T） | 三行 104/105/105px，文字 49px ✓ |
| 周二 3 门（荣耀） | 三行 105/105/106px，文字 53px ✓（修复前 113/113/**90**，文字被裁） |
| **周三 1 门** | 1 行 + **2 个空格占位** ✓ 行高没被撑大 |
| **周四 4 门（满课）** | 2 门 + 「还有 2 门课…」，三行等高不裁 ✓（小组件只够 3 行） |
| **周六 0 门（空状态）** | 「今日无课」居中占满 368px，列表容器收起 ✓ |

验完记得把测试机 `settings put global auto_time 1` 恢复自动对时。
读小组件**不要按 HOME / 不要滑动**——停在那一页直接 `uiautomator dump` 就行（按 HOME 会把界面顶走，滑动可能打乱用户摆好的桌面）。

⚠️ **踩坑（自己引入又修掉的）**：空格最初写成了 `<View>`，
但 **RemoteViews 只允许有限几种控件，`android.view.View` 不在白名单**，
会导致布局 `InflateException: Class not allowed to be inflated`，
启动器直接报「無法新增小工具」。
已全部换成 `LinearLayout`。**以后往 `widget_*.xml` 加控件前先确认在 RemoteViews 白名单内**
（FrameLayout / LinearLayout / RelativeLayout / TextView / ImageView / Button / ProgressBar / ListView / …）。

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

## DSH 图像输入（2026-09-15 已开）

Flash 4.1（`deepseek-flash`）**上游其实支持图像**（实测红/蓝纯色图能正确识别），
但 `~/.dsh/settings.yaml` 里这条模型没声明 `inputModalities`，DSH 就按纯文本处理
（`dsh-llm-deepseek/lib/index.js:1286` 默认 `["text"]`，`:1585` 会把图片换成占位文本）。

已在 settings.yaml 给 `deepseek-flash` 和运行时快照 id `deepseek-v4.1-flash-expires-on-0910`
补上 `inputModalities: [text, image]` + `imagePixelBudget/imageMaxBytes`，**重启 dsh web 后生效**（已验证）。
备份：`~/.dsh/settings.yaml.bak-20260914134724`。

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
