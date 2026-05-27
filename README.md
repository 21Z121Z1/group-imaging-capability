# 集团影像能力 v1

Android 离线 OPPO/Oplus 摄影库归类与归档应用。应用在设备本地扫描 MediaStore 图片，解析 EXIF 与 Oplus `UserComment`，建立 CaptureSession 索引，按设备、镜头、焦段、日期、RAW/Live 和拍摄模式做归类，并支持在移动原文件前预览命中文件与 RAW 伴随项。

## 结构

- `app/`: Android 应用模块，包含 Room、扫描内核、Compose UI。
- `app/src/sharedTest/`: 共享 fake、fixture、scenario builder、robot。
- `app/src/androidTest/contract/`: 直接挂载 screen composable 的 UI 契约测试。
- `app/src/androidTest/integration/`: `MainActivity` + `AppRoot` + Hilt fake binding 的集成测试。
- `app/src/test/`: JVM 单测与 Roborazzi 截图回归测试。
- `fixtures/`: 从 `/Volumes/42APFS/x9` 生成的真值样本。
- `tools/generate_truth_fixture.py`: 用现有 Python decoder 生成 fixture 的脚本。

## 运行前提

- Android Studio / Gradle Wrapper
- Android SDK 35
- `JAVA_HOME` 指向 JDK 17
- 研发真值对照依赖本机已安装 `exiftool`

## 当前可交付范围

- Room 本地索引，使用 generation 保护全量扫描失败时的旧索引。
- WorkManager 前台扫描任务，支持页面离开后继续执行。
- MediaStore 扫描、EXIF 解析、Oplus 原拍判定、镜头归类、CaptureSession 聚合。
- Home / Calendar / Insights / Albums / Settings / Failed Items Compose UI。
- Calendar 年、月、日聚合视图，并可跳转到 Insight 的对应年份、月份或精确日期筛选。
- Insight 年份、月份、日期、设备、镜头、焦段、RAW、Live 筛选。
- Insight 月度/年度趋势同时提供图形和可读标签/数量，避免趋势区只有无文字图形。
- 规则预览：展示命中文件、真实缩略图或稳定兜底缩略图、RAW/Live 标记。
- 移动原文件：按规则在 `DCIM/myalbums/<规则名>/` 下建立目标相册目录，持久化 move plan / candidate，包含 RAW 伴随项、同名冲突改名、MediaStore 写授权和移动审计。

## 发布前注意事项

- Android 14+ 可能只授予 Selected Photos Access；扫描结果只代表系统授权给本应用的媒体范围。
- Android 11+ 移动非本应用创建的媒体需要系统写入授权；大批量授权请求会按批次发起，授权请求使用图片媒体项 URI。
- 本地数据库包含媒体路径、文件名、解析摘要和归类结果；备份规则只包含 shared preferences，不备份本地索引数据库。
- `release` 构建当前使用 debug 初始化的 `releaseLike` 签名，仅适合作为本地交付/内测样包；正式发布需要替换为真实 release keystore。
- 物理移动依赖 MediaStore URI 和授权结果；没有 root 要求。

## Harness Engineering

### 测试分层

- `JVM 单测`
  - 覆盖 ViewModel、解析器、仓储 helper 与 Room migration。
  - `sharedTest` 的 `FakeInsightRepository`、`HarnessScenario`、`UiStateFixtures` 同时服务 unit test 与 androidTest。
- `Screen contract tests`
  - 直接挂 `Screen` composable。
  - 优先使用 `TestTags` 与稳定语义节点，不再依赖文本选择器驱动交互。
- `App integration tests`
  - 使用 `MainActivity` 和真实 `AppRoot` 导航。
  - instrumentation runner 切到 `HiltTestApplication`，并通过 `@TestInstallIn` 注入 fake repository。
  - 默认开启 Compose accessibility checks、Android Test Orchestrator 与 `clearPackageData`。
- `Screenshot regression`
  - 基于 Roborazzi，覆盖 light/dark、large font 和关键空态/完成态。
  - golden 文件输出在 `app/src/test/screenshots/`。

### 关键目录

- `app/src/main/java/com/oplus/groupimaging/testing/TestTags.kt`: 生产代码与测试共享的 selector 合约。
- `app/src/sharedTest/java/com/oplus/groupimaging/testing/scenario/HarnessScenario.kt`: app 级集成场景 builder。
- `app/src/sharedTest/java/com/oplus/groupimaging/testing/robot/ComposeHarness.kt`: 共享 robot 与 Compose harness helper。
- `app/src/androidTest/java/com/oplus/groupimaging/testing/hilt/`: instrumentation runner 与 fake test module。
- `app/src/test/java/com/oplus/groupimaging/testing/screenshot/`: Roborazzi 截图测试。

## 测试命令

### 本地基础校验

- `./gradlew testDebugUnitTest`
- `./gradlew compileDebugAndroidTestKotlin`
- `./gradlew lintDebug`
- `./gradlew assembleDebug assembleRelease`

### 截图回归

- 首次录制 golden: `./gradlew app:recordRoborazziDebug -Pscreenshot=true`
- 校验现有 golden: `./gradlew app:verifyRoborazziDebug -Pscreenshot=true`

### Instrumentation

- 连接真机或模拟器: `./gradlew app:connectedDebugAndroidTest`
- 使用 Gradle Managed Device: `./gradlew app:pixel8Api35DebugAndroidTest`
- 跑所有 managed device 组: `./gradlew app:phoneGroupDebugAndroidTest`

## CI

- GitHub Actions 流水线定义在 `.github/workflows/android-harness.yml`。
- PR 默认分三段执行：
  - `testDebugUnitTest`
  - `verifyRoborazziDebug -Pscreenshot=true`
  - `pixel8Api35DebugAndroidTest`
- benchmark / baseline profile 还未纳入第一阶段 gating。

## 已知限制

- 本机如果没有可用 emulator system image，`pixel8Api35DebugAndroidTest` 会在设备准备阶段失败。
- Roborazzi golden 需要显式录制后再进入 verify 流程。
- 物理移动原文件基于 MediaStore `DISPLAY_NAME` / `RELATIVE_PATH` 更新；受 Android 存储权限限制时会用 `content://media/external/images/media/{id}` 形式的图片媒体 URI 请求授权或记录失败审计。
- 当前还不是 Play Store 正式上架包：需要正式签名、隐私文案、目标设备矩阵和真实设备回归。
