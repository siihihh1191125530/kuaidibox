# 构建、测试与发布

## 环境

- JDK 17（本地也使用 JDK 21 验证）
- Android SDK 34、Build Tools 34.0.0
- 仓库自带 Gradle 8.5 Wrapper

在 `local.properties` 设置 `sdk.dir`，或设置 `ANDROID_HOME`。密钥、密码、SDK 路径和构建产物不提交到仓库。

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease
```

Windows 使用 `gradlew.bat`。Debug 输出在 `app/build/outputs/apk/debug`，Release 默认输出未签名包，必须完成签名才能安装。

## 设备测试

使用专用测试模拟器，避免影响真实短信和应用数据。设备测试包含系统小组件 Host，需要临时授权绑定：

```bash
./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell appwidget grantbind --package com.example.expresshelper --user 0
./gradlew :app:connectedDebugAndroidTest
adb shell appwidget revokebind --package com.example.expresshelper --user 0
```

即使测试失败，也须执行 `revokebind`（脚本中使用 `finally`）。测试创建并清理专用临时 Host，不修改已有桌面。`UiFixtureTest` 默认不生成演示数据。

## 发布约定

1. 更新版本名、递增 `versionCode`，更新 CHANGELOG。
2. 完成构建、单元测试、Lint 和设备功能测试，记录未验证范围。
3. 使用已有发布密钥签名 Release APK；不要为每个版本重新生成密钥，也不要把 Debug 包作为正式包。
4. 使用 `apksigner verify --verbose --print-certs` 检查证书、使用 `aapt dump badging` 检查包名和版本。
5. 提交源码，给该提交创建独立版本标签；不要移动已经发布的标签。
6. 在 GitHub Releases 附上签名 APK、`SHA256SUMS.txt` 和对应更新说明。源码由 GitHub 根据标签生成。
7. 发布后重新下载附件校验 SHA-256，检查 README 下载入口、截图与安装说明。

`v1.0.0-rebuild.1` 是重新发布的 1.0，与旧版签名不同。今后必须保管好重制版发布密钥及密码，才能覆盖升级。仓库只公开证书指纹，不公开私钥。

GitHub Actions 自动执行构建、JVM 测试和 Lint，输出的 Debug artifact 供开发验证，不是正式发布渠道。设备测试在本地模拟器执行。

## 发布签名

1.0 重制版证书 SHA-256：

```text
0a1d07273eda51fb19ea9dfd3b78827cdcdd16af46197c8596070822be99ffbc
```

该指纹与原版 v1.0.0 不同。它用于确认后续 APK 的签名身份，不是 APK 文件的 SHA-256；文件校验值随版本变化，见各 Release 的 SHA256SUMS.txt。
