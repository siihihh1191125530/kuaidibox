# 快递盒 1.0 重制版设计说明

面向日常取快递的安卓用户，首要任务是快速看清号码、找到驿站、标记已取。

## Skill 调研（2026-11-13）

GitHub API 查询到的仓库总 stars：
- [anthropics/skills](https://github.com/anthropics/skills)：193548。使用 frontend-design/SKILL.md 的设计流程。
- [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)：148533。候选，适合移动端交互及可访问性参考。
- [Impeccable](https://github.com/pbakaus/impeccable)：108805。候选，面向设计审查和打磨。

这是高星候选对比，不声称覆盖所有仓库的完整榜单；合集 stars 不等于单个 skill stars。按当前项目读取和应用 frontend-design，没有更改全局 skill。

## 设计系统

主题：取件票。清楚的号码与信息优先级；不做营销页、仪表盘或装饰性渐变。
- 主色 #2458C6；背景 #F1F5FB；正文 #152744；次级文字 #51627C；表面 #FFFFFF；分隔线 #DAE2EE。
- 中文使用系统字体，避免联网字体。取件码使用系统等宽字体，便于分辨字母数字。
- 标题 26–30sp，号码 32sp，正文 14–16sp，辅助信息不小于 12sp。
- 页面左右留白 20–24dp；操作目标最小 48dp；设置内容自然增长并可滚动。
- 深色模式使用独立颜色资源，保留相同层级。

布局：标题 → 待取件数 → 短信状态与补查 → 按时间排列的取件票。
取件票：驿站与时间 → 大号码 → 地址 → 已取操作。
设置：短信 → 识别规则 → 记录与数据 → 关于。

## 审视与取舍

取件码是唯一强调的大字号元素，避免无意义的统计大卡片。权限入口显示授权状态，不宣称已获得厂商后台运行保证。保留原生 Android 布局与交互。

桌面小组件 widget_info.xml 和 widget_express_list.xml 原样保留。仅修复异步刷新、点击与多个实例的页码行为。
