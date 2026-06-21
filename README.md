# 药爱健康 (AIHealthManager)

面向居家健康管理场景的 Android 应用，集成 AI 健康分析、药品 OCR 识别、用药提醒、跌倒检测，以及 OPPO 健康数据同步。

## 功能特性

- **健康数据看板**：展示心率、睡眠、血糖等健康指标，支持历史趋势图表
- **OPPO 健康同步**：通过 HeyTap Health SDK 读取穿戴设备数据（需 OPPO/一加设备）
- **药品扫描**：基于 ML Kit OCR 识别药盒文字，结合 DeepSeek AI 提取药品名称
- **用药管理**：药品清单、服药提醒、过期检查
- **AI 健康报告**：调用 DeepSeek API 生成个性化健康分析
- **跌倒检测**：后台加速度传感器监测，触发告警并可发送短信通知紧急联系人
- **语音播报**：TTS 欢迎语与健康提示

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 16 (API 36) |
| OCR | Google ML Kit Text Recognition (含中文增强) |
| AI | DeepSeek Chat API |
| 图表 | MPAndroidChart |
| 网络 | OkHttp + Gson |
| 健康数据 | OPPO HeyTap Health SDK 2.1.7 |

## 环境要求

- Android Studio Ladybug 或更高版本
- JDK 11+
- Android SDK（API 36）
- DeepSeek API Key
- OPPO Health SDK Maven 凭据（用于拉取 `com.heytap.health:sdk`）

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/<your-username>/AIHealthManager_2.git
cd AIHealthManager_2
```

### 2. 配置本地密钥

复制配置模板并填入你的密钥：

```bash
cp local.properties.example local.properties
```

编辑 `local.properties`：

```properties
sdk.dir=/你的/Android/sdk/路径
DEEPSEEK_API_KEY=你的_DeepSeek_API_Key
HEYTAP_HEALTH_MAVEN_USER=你的_Maven_用户名
HEYTAP_HEALTH_MAVEN_PASSWORD=你的_Maven_密码
```

> `local.properties` 已在 `.gitignore` 中，不会被提交到 Git。

### 3. 构建与运行

```bash
./gradlew assembleDebug
```

或在 Android Studio 中直接打开项目并运行。

### 4. 运行单元测试

```bash
./gradlew testDebugUnitTest
```

## 项目结构

```
app/src/main/java/com/example/aihealthmanager_2/
├── MainActivity.kt          # 主界面：健康看板、药品扫描、用药管理
├── HistoryActivity.kt       # 健康历史记录
├── AIService.kt             # DeepSeek AI 接口封装
├── FallDetectionService.kt  # 跌倒检测前台服务
├── MedicineItem.kt          # 药品数据模型
├── MedicationRecord.kt      # 服药记录模型
├── DailyHealthData.kt       # 每日健康数据模型
└── ScanResult.kt            # OCR 扫描结果模型
```

## 权限说明

应用需要以下权限：

| 权限 | 用途 |
|------|------|
| `CAMERA` | 拍摄药盒进行 OCR 识别 |
| `INTERNET` | AI 接口调用与数据同步 |
| `SEND_SMS` | 跌倒告警时通知紧急联系人 |
| `ACCESS_FINE_LOCATION` | OPPO 健康 SDK 要求 |
| `POST_NOTIFICATIONS` | 服药提醒与跌倒告警通知 |
| `FOREGROUND_SERVICE` | 跌倒检测后台服务 |

## 注意事项

- DeepSeek API Key 和 OPPO Maven 凭据**切勿**提交到版本库
- OPPO 健康功能需在安装了「健康」应用的 OPPO/一加/真我设备上使用
- 跌倒检测为辅助功能，不能替代专业医疗监护设备
- 若 API Key 已泄露，请尽快在 [DeepSeek 平台](https://platform.deepseek.com/) 轮换密钥

## License

本项目仅供学习与交流使用。
