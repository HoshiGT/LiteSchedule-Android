# LiteSchedule Memo

## 当前状态

- App 名：`LiteSchedule`
- 版本：`1.0.0`
- GitHub：https://github.com/HoshiGT/LiteSchedule-Android
- 已发布 Release：`v1.0.0`

## 核心功能

- 周课表网格（ViewPager2 翻页）
- 当前周次切换 + 左右滑动吸附
- 按课程周次过滤（例：1-8 周 A，9-16 周 B）
- 自定义开学日期
- 自定义上课时间
- 点击课表空白处快速添加课程（气泡 + 拖钮）
- 新增课程支持名称、颜色、周次、节次、教师、教室、备注
- 教务导入：
  - 自定义学校网址
  - 支持加载教务系统页面
  - 支持导出 XLS / XML 自动识别导入
  - 导入成功后自动回主页
- 今日课程桌面小组件（可滚动、圆角气泡、可调尺寸）
- 上课提醒（AlarmManager + 通知）
- 自定义背景（预设 + 相册图片）
- 10/50/100 天赞赏提醒
- 在线解锁 / 赞赏页：https://schedule.hoshichan.moe
- 开源仓库 / 赞赏入口已分离

## 构建

```bash
cd /home/hoshi/Kebiao/qk_java
# 详细构建命令见 HANDOFF.md，包含 AndroidX 资源合并和 R.java 生成
```

## 已知问题 / 后续

- 教务 WebView 的顶部 logo 栏在不同 UA 下显示仍有差异；当前提供“手机版 / 电脑版”切换
- 桌面小组件目前是列表式今日课程；更复杂的“桌面直接拖拽添加课程”需要悬浮窗方案
- 赞赏/在线解锁服务在 `<服务器IP>`，API 走 schedule.hoshichan.moe
- 新的问题建议新开窗口，携带本文件继续
