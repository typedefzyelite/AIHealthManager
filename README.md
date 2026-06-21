# 药爱健康

Android 健康管理应用，主要功能：健康数据看板、药品 OCR 扫描、用药提醒、跌倒检测、OPPO 健康数据同步。

## 功能

- 健康看板：心率、睡眠、血糖等指标，带历史趋势图
- 药品扫描：ML Kit OCR + DeepSeek 提取药名
- 用药管理：药品清单、服药提醒、过期检查
- 跌倒检测：加速度传感器监测，可短信通知紧急联系人
- OPPO 健康同步：通过 HeyTap Health SDK 拉取穿戴设备数据
- AI 健康报告：DeepSeek 生成分析

## 技术栈

Kotlin · ML Kit · DeepSeek API · OkHttp · Gson · MPAndroidChart · OPPO HeyTap Health SDK 2.1.7

minSdk 24 · targetSdk 36

## 跑起来

```bash
git clone https://github.com/typedefzyelite/AIHealthManager.git
cd AIHealthManager
cp local.properties.example local.properties
```

在 `local.properties` 里填好 SDK 路径、DeepSeek API Key 和 OPPO Maven 账号密码，然后：

```bash
./gradlew assembleDebug
```

也可以用 Android Studio 直接打开。

## 目录

```
app/src/main/java/com/example/aihealthmanager_2/
├── MainActivity.kt
├── HistoryActivity.kt
├── AIService.kt
├── FallDetectionService.kt
├── MedicineItem.kt
├── MedicationRecord.kt
├── DailyHealthData.kt
└── ScanResult.kt
```

## 权限

| 权限 | 用途 |
|------|------|
| CAMERA | 拍药盒做 OCR |
| INTERNET | 调 AI 接口、同步数据 |
| SEND_SMS | 跌倒告警发短信 |
| ACCESS_FINE_LOCATION | OPPO 健康 SDK 需要 |
| POST_NOTIFICATIONS | 服药/跌倒通知 |
| FOREGROUND_SERVICE | 跌倒检测后台服务 |

## 其他

- OPPO 健康功能需要 OPPO/一加/真我设备，且装了「健康」App
- 跌倒检测只是辅助，不能当医疗设备用
