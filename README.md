# Veil

把读书笔记一键变成可执行的任务或日程。

## 快速开始

### 1. 配置 API Key

在 `local.properties` 中添加：
```properties
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

### 2. 运行

Android Studio 打开项目直接点击 ▶ Run，或命令行：
```bash
./gradlew installDebug
```

## 技术栈

| 模块 | 方案 |
|------|------|
| UI | Jetpack Compose (Material3) |
| 架构 | MVVM (Activity + ViewModel + StateFlow) |
| 数据库 | Room 2.6.1 + Flow 响应式 |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson |
| 异步 | Kotlin Coroutines |
| 提醒 | AlarmManager + NotificationCompat |

## 权限

| 权限 | 说明 |
|------|------|
| `INTERNET` | 调用 DeepSeek API |
| `POST_NOTIFICATIONS` | Android 13+ 发送提醒通知 |
| `SCHEDULE_EXACT_ALARM` | Android 12+ 精确闹钟 |
| `RECEIVE_BOOT_COMPLETED` | 开机后恢复闹钟 |

## 配置

- 依赖仓库使用**阿里云镜像**加速
- Gradle 分发包使用**腾讯云镜像**
- API Key 通过 `local.properties` → `BuildConfig` 注入，不提交到 Git
