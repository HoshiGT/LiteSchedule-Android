# 交接文档 / Handoff

## 一、当前状态

### 手机/环境
- 设备：OnePlus 8T (KB2000)，已 root，无线 ADB `192.168.8.107:40877`
- 已安装：
  - `com.suda.yzune.wakeupschedule` —— 魔改版 WakeUp课程表（原包名，新签名，能正常打开教务网页）
  - `com.hoshi.qingkebiao` —— 新开发的“轻课表” v0.1 原型
- 已卸载：
  - `com.hoshi.wakeupmin`（之前换包名导致 WebView 坏掉的版本）
  - 官方原版 WakeUp课程表（为了装魔改版被卸载）

### 现有 APK 产物
- `/home/hoshi/Kebiao/wakeup_local_min.apk`
  - 魔改版 WakeUp 课程表
  - 包名：`com.suda.yzune.wakeupschedule`
  - 签名：`wakeup-local.keystore`（密码 `localwakeup`）
  - 功能：
    - 去广告 / 去推送 / 去大部分联网组件
    - 学校列表只保留西安理工
    - 内置 WebView 正常，可打开教务登录页
    - 西安理工进入内部浏览器时强制使用 WebVPN HTTPS 地址
    - 原“捐赠”弹窗已加入 Hoshi 赞赏二维码
- `/home/hoshi/Kebiao/qingkebiao_v0.1.apk`
  - 新开发轻课表
  - 包名：`com.hoshi.qingkebiao`
  - 功能：
    - 手动添加课程
    - 列表展示/删除
    - 文件导入（CSV：课程名,教师,教室,周几,开始节,结束节,周次）
    - 教务导入（WebView 打开 WebVPN）
    - 今日课程小组件
    - 设置中“上课提醒”开关（已接 AlarmManager 通知，Android 13 首次开启时请求通知权限；如无精确闹钟权限会引导去系统设置开启，未开启时自动退化到 setAndAllowWhileIdle）
    - 首轮外观优化：渐变顶部栏、课程卡片列表、圆角按钮/输入框、应用图标
    - 主页已改为周课表网格（时间 x 星期），课程按节次色块显示，点击可查看/删除
    - 导入入口移到设置页；主页无课程时居中显示空状态，可跳设置导入
    - 新增当前周次切换（◀/▶）和按周次过滤课表（如 1-8 周 A、9-16 周 B）
    - 周次切换已改为横向翻页：像手机桌面一样左右滑动切换周次，20 周预生成页面 + 滑动吸附
    - 课表改为参考 WakeUp 的 FrameLayout 覆盖式布局：左侧“上课时间”列 + 7 列星期并显示日期 + 课程气泡按节次跨行，外面只显示课程名/教室，老师在详情
    - 新增“调整上课时间”设置页，可自定义每节课开始时间
    - 新增“设置开学日期”，主页显示“实际本周：第N周”；周次翻页和日期都基于开学第一周周一计算
    - 设置入口改为顶部齿轮；新增“赞赏 1 元解锁自定义背景”（微信赞赏二维码 + 设备码兑换激活码 + 预设背景切换 + 相册导入图片）
    - 设置页新增“开源 / 赞赏”入口，跳转到 `open_source_url`（当前 `https://ai.hoshichan.moe`）
    - 教务导入页新增“导入当前课表”按钮，可尝试从 WebView 当前页面解析课表表格写入本地数据库
    - WebView 已加 `DownloadListener`：点击教务“导出课表”时自动下载文件，若是 XML 会自动解析导入本地数据库
    - 教务导入页改为先打开 WebVPN 登录页，另加“打开教务课表”按钮进入内部课表页，避免直接加载 API 返回 JSON 未登录提示
    - 文件导入已支持 XML / XLS：教务系统导出的课表 XML 可用 `CourseXmlParser` 识别，课表 .xls 可用 `CourseXlsParser` + `libs/jxl.jar` 解析；CSV 仍保留
    - 基于 Java + Android 原生，不用 Gradle，可以纯命令行构建（已内置 AndroidX ViewPager2/JXL jar）

## 二、魔改 WakeUp 版本的关键信息

源码目录：`/tmp/wakeup2/apk`（apktool 解码后的可重新打包目录）
- 构建命令：
  ```bash
  cd /tmp/wakeup2
  PATH=/home/hoshi/Android/Sdk/build-tools/35.0.0:$PATH \
  java -cp '/usr/share/apktool/*' brut.apktool.Main --advance b --use-aapt2 \
    -a /home/hoshi/Android/Sdk/build-tools/35.0.0/aapt2 \
    -o wakeup_local_min.apk /tmp/wakeup2/apk
  ```
- 签名命令：
  ```bash
  apksigner sign --ks /home/hoshi/Kebiao/wakeup-local.keystore \
    --ks-pass pass:localwakeup --key-pass pass:localwakeup \
    --out wakeup_local_min_signed.apk wakeup_local_min.apk
  ```
- 注意：
  - 包名必须保留 `com.suda.yzune.wakeupschedule`，改成别的包名会导致内置 WebView 无法联网/加载失败（已实测）。
  - `WebViewLoginFragment.smali` 中已写入 WebVPN 强替逻辑。
  - 官方原包不能与魔改版共存，因为包名相同、签名不同。
  - 赞赏码：`/home/hoshi/Kebiao/mm_reward_qrcode_1788096589005.png`，已放入 `fragment_donate.xml`。

## 三、轻课表新项目

目录：`/home/hoshi/Kebiao/qk_java`

### 纯命令行构建流程（不需要 Gradle）
```bash
cd /home/hoshi/Kebiao/qk_java

# 1. 编译资源
/home/hoshi/Android/Sdk/build-tools/35.0.0/aapt2 compile --dir res -o build/res.zip

# 2. 链接资源 + 生成 R.java（AndroidX 资源作为 overlay 合并）
/home/hoshi/Android/Sdk/build-tools/35.0.0/aapt2 link \
  -I /home/hoshi/Android/Sdk/platforms/android-36/android.jar \
  -R build/res.zip \
  -R ax_res_lib/core-1.9.0.zip \
  -R ax_res_lib/recyclerview-1.2.1.zip \
  -R ax_res_lib/viewpager2-1.0.0.zip \
  --manifest AndroidManifest.xml --auto-add-overlay \
  --java gen -o build/base.apk \
  --output-text-symbols /tmp/qk_symbols.txt \
  --min-sdk-version 26 --target-sdk-version 36 \
  --version-code 1 --version-name 0.1.0
# 然后用 python 脚本按 /tmp/qk_symbols.txt 生成 gen/androidx/{recyclerview,core,viewpager2}/R.java

# 3. 编译 Java（需要 libs/*.jar，包含 jxl 和 AndroidX）
CP="/home/hoshi/Android/Sdk/platforms/android-36/android.jar:$(find libs -name '*.jar' | tr '\n' ':')" && \
javac -source 17 -target 17 -classpath "$CP" \
  -d classes $(find gen -name 'R.java') src/com/hoshi/qingkebiao/*.java

# 4. dex
/home/hoshi/Android/Sdk/build-tools/35.0.0/d8 \
  --lib /home/hoshi/Android/Sdk/platforms/android-36/android.jar \
  --release --output build $(find classes -name '*.class') $(find libs -name '*.jar')

# 5. 塞 dex 进 APK
cd build && zip -q base.apk classes.dex && cd ..

# 6. 签名
/home/hoshi/Android/Sdk/build-tools/35.0.0/apksigner sign \
  --ks /home/hoshi/Kebiao/wakeup-local.keystore \
  --ks-pass pass:localwakeup --key-pass pass:localwakeup \
  --out qingkebiao.apk build/base.apk
```

### 源码文件
- `src/com/hoshi/qingkebiao/Course.java`
- `src/com/hoshi/qingkebiao/CourseDatabase.java`
- `src/com/hoshi/qingkebiao/MainActivity.java`
- `src/com/hoshi/qingkebiao/SettingsActivity.java`
- `src/com/hoshi/qingkebiao/ImportActivity.java`
- `src/com/hoshi/qingkebiao/WebImportActivity.java`
- `src/com/hoshi/qingkebiao/TodayWidgetProvider.java`
- `src/com/hoshi/qingkebiao/BootReceiver.java`
- `src/com/hoshi/qingkebiao/TimeTable.java`
- `src/com/hoshi/qingkebiao/TimeSettingsActivity.java`
- `src/com/hoshi/qingkebiao/WeekDateManager.java`
- `src/com/hoshi/qingkebiao/ReminderScheduler.java`
- `src/com/hoshi/qingkebiao/ReminderReceiver.java`
- `src/com/hoshi/qingkebiao/CourseAdapter.java`
- `src/com/hoshi/qingkebiao/BackgroundManager.java`
- `src/com/hoshi/qingkebiao/BackgroundActivity.java`
- `src/com/hoshi/qingkebiao/UnlockManager.java`
- `src/com/hoshi/qingkebiao/OnlineUnlockManager.java`
- `src/com/hoshi/qingkebiao/CourseXmlParser.java`
- `src/com/hoshi/qingkebiao/CourseXlsParser.java`
- `libs/jxl.jar`（读取 .xls 用）
- `libs/viewpager2-1.0.0.jar`、`libs/recyclerview-1.2.1.jar`、`libs/core-1.9.0.jar`、`libs/customview-1.1.0.jar`、`libs/collection-1.2.0.jar`、`libs/annotation-1.6.0.jar`（AndroidX 翻页依赖）
- `ax_res_lib/`（AndroidX AAR 资源包，已按单独 zip 编译进链接）
- 周次翻页已改为 `ViewPager2`，跟 WakeUp 一样使用系统级翻页/吸附

### 已实现
- 手动添加课程（课程名、教师、教室、周几、节次、周次）
- 列表展示/删除
- 文件导入 CSV / XML / XLS
- 教务导入 WebView 打开 WebVPN
- 今日课程小组件
- 设置提醒开关（已接 AlarmManager 通知）

### 待办 / 下一步
1. ~~提醒~~（已完成）
   - 默认节次时间换算：`TimeTable.java`
   - `AlarmManager` 定时通知：`ReminderScheduler.java`、`ReminderReceiver.java`
   - 设置页开关已接入，开启/关闭会重排/取消闹钟；删除课程会取消对应提醒
2. 教务导入：
   - 在 WebVPN WebView 里做登录
   - 抓取课表 HTML/JSON 解析成课程列表
   - 导入后写入本地数据库
   - 已加“导入当前课表”按钮 + 通用表格 JS 解析首版，实际页面结构需真机适配
   - 已实测教务导出的是 .xls（JXL 格式），现在会自动识别并解析；XML/CSV 作为备用
3. 周课表 UI：
   - 已完成首版周课表网格：时间 x 星期行列显示，课程色块+点击详情/删除
   - 后续可继续做合并节次跨行、自定义节次时间
4. 小组件：
   - 多种样式/尺寸
   - 点击跳转到 App
5. 美化/图标/App 名：
   - 首轮外观优化已完成：主题色、渐变头部、课程卡片列表、圆角按钮/输入框、图标
   - 设置已改为顶部齿轮入口
   - 赞赏解锁自定义背景：1 元二维码 + 在线服务端验证 + 离线激活码回退 + 5 套渐变背景 + 支持相册导入图片作为背景
   - 后续可结合周课表网格继续调整

## 四、已知坑
- `/home` 挂载是只读的，Gradle 默认用户目录不可写，所以新项目改为纯命令行构建，不用 Gradle。
- 目前赞赏解锁是“在线优先、离线回退”方案：优先请求 `https://ai.hoshichan.moe/api/qingkebiao/unlock`；服务端未接入或断网时，回退到设备码/激活码离线解锁。离线 secret 写在 APK 里，防君子不防逆向；后续服务端接口稳定后可考虑只留联网验证。
- 在线解锁服务已部署在 `<服务器IP>`：
  - Node 服务：`/opt/qk-server/server.js`
  - 配置：`/opt/qk-server/config.json`（内含 adminToken）
  - 数据：`/opt/qk-server/unlocks.json`
  - systemd：`qk-unlock.service`
  - Nginx 路径：`/api/qingkebiao/` 反代到 `127.0.0.1:8765`
  - 管理解锁：`POST /api/qingkebiao/mark?token=<adminToken>&device=<设备码>`
  - 支付回调预留：`POST /api/qingkebiao/pay/notify`，校验 `sign=HMAC-SHA256(paySecret, device|status)`，`status=success` 自动解锁
- 服务端 API 暂按约定：
  - `GET /api/qingkebiao/unlock?device=DEVICE` -> `{"unlocked":true|false}`
  - `POST /api/qingkebiao/unlock` body `{"device":"DEVICE","proof":"......"}` -> `{"unlocked":true|false}`
  - 若实际接口不同，改 `OnlineUnlockManager.java` 和 `strings.xml` 中的 `server_unlock_url` 即可。
- 如果继续用 Gradle，需要把 `GRADLE_USER_HOME` 指到 `/home/hoshi/Kebiao/.gradle`，但当前没有 Gradle 发行包缓存，离线构建会卡在下载。
- 魔改版不要改包名，否则 WebView 会坏。
- 官方版和魔改版不能共存。
