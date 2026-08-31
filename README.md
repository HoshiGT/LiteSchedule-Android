# LiteSchedule

一个轻量、开源的 Android 课程表应用。

GitHub: <https://github.com/HoshiGT/LiteSchedule-Android>

## 功能

- 周课表网格视图（参考 WakeUp 风格）
- 按周次过滤课程（支持 1-8 周 A / 9-16 周 B 这类情况）
- 左右滑动切换周次（ViewPager2）
- 自定义开学日期、显示实际本周周数
- 自定义每节课开始时间
- 课程数据导入：
  - CSV
  - XML
  - 教务系统导出的 XLS
- 设置页：
  - 导入入口
  - 上课提醒（AlarmManager + 通知）
  - 自定义背景（预设 + 相册导入）
  - 开源 / 赞赏入口
- 无 Gradle，纯命令行构建

## 构建

参考 [HANDOFF.md](HANDOFF.md) 中的构建流程，主要依赖：

- Android SDK 35/36
- `libs/` 下的 AndroidX ViewPager2、RecyclerView、Core、JXL 等 jar
- `ax_res_lib/` 下已提取的 AndroidX 资源包

## 目录

- `qk_java/`：Android 源码与资源
- `HANDOFF.md`：开发交接文档、构建命令、已知坑
