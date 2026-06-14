# Veil

把读书笔记一键变成可执行的任务或日程。

## 核心流程

```
分享笔记 → AI 分析建议 → 你确认 → 生成任务/日程提醒
```

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

## 项目结构

```
app/src/main/java/com/lemon/veil/
├── MainActivity.kt            # 入口，处理分享 Intent + UI 组件
├── MainViewModel.kt           # 业务逻辑：AI 调用、数据库操作、状态管理
│
├── data/
│   ├── AppDatabase.kt         # Room 数据库单例
│   ├── NoteEntity.kt          # 笔记实体（原文、建议、类型、时间、完成状态）
│   └── NoteDao.kt             # 增删改查接口
│
├── ai/
│   ├── ApiClient.kt           # Retrofit 客户端，从 BuildConfig 读取 API Key
│   ├── DeepSeekApi.kt         # API 接口定义（chat/completions）
│   └── SuggestionResponse.kt  # AI 返回的 JSON 模型
│
└── utils/
    └── AlarmScheduler.kt      # AlarmReceiver + 闹钟调度 + 通知渠道
```

## 文件职责速查

| 文件 | 职责 |
|------|------|
| `MainActivity` | 接收 `ACTION_SEND` 分享文本，交给 ViewModel 分析；渲染 Compose UI |
| `MainViewModel` | 调用 DeepSeek API → 解析 JSON → 弹出确认弹窗 → 存入数据库 → 设定闹钟 |
| `NoteEntity` | 数据库表结构：id / originalText / suggestion / type / suggestedTime / createdAt / isCompleted |
| `NoteDao` | Flow 响应式查询 + 增删改 |
| `AppDatabase` | Room 单例，数据库名 `note_database` |
| `ApiClient` | Retrofit + OkHttp，自动附加 Bearer Token |
| `DeepSeekApi` | 定义 `suspend fun getSuggestion()` 接口 |
| `SuggestionResponse` | 字段：suggestion, type, suggested_time |
| `AlarmScheduler` | `scheduleAlarm()` 设闹钟 / `cancelAlarm()` 取消 / `createNotificationChannel()` 初始化 |
| `AlarmReceiver` | BroadcastReceiver，到点弹出 Notification |

## 数据流

```
分享文本 (Intent.ACTION_SEND)
    ↓
MainActivity.handleIntent()
    ↓
MainViewModel.processSharedText()
    ├── 构建 ChatRequest (system prompt + 用户文本)
    ├── 调用 ApiClient.deepSeekApi.getSuggestion()
    ├── 解析 JSON → 生成 NoteEntity
    └── 更新 UiState.showDialog = true
    ↓
MainScreen 检测到 showDialog → 弹出 SuggestionDialog
    ↓
用户点击「同意，生成」
    ├── confirmSuggestion() → noteDao.insertNote()
    └── 如果 type == "日程" → AlarmScheduler.scheduleAlarm()
    ↓
Room Flow 触发 → UI 自动刷新显示新记录
```

## 技术栈

| 模块 | 方案 |
|------|------|
| UI | Jetpack Compose (Material3) |
| 架构 | MVVM (Activity + ViewModel + StateFlow) |
| 数据库 | Room 2.6.1 + Flow 响应式 |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson |
| 异步 | Kotlin Coroutines (viewModelScope) |
| 提醒 | AlarmManager + NotificationCompat |

## 权限

| 权限 | 说明 |
|------|------|
| `INTERNET` | 调用 DeepSeek API |
| `POST_NOTIFICATIONS` | Android 13+ 发送提醒通知 |
| `SCHEDULE_EXACT_ALARM` | Android 12+ 精确闹钟 |
| `RECEIVE_BOOT_COMPLETED` | 开机后恢复闹钟（预留） |

## Gradle 配置

- 依赖仓库使用**阿里云镜像**加速
- Gradle 分发包使用**腾讯云镜像**
- API Key 通过 `local.properties` → `BuildConfig.DEEPSEEK_API_KEY` 注入，不提交到 Git

## AI Prompt

System Prompt 定义在 `MainViewModel.getSystemPrompt()`，要求 AI：
1. 理解笔记核心意思
2. 给出 1 条实用行动建议（≤40 字）
3. 判断类型：任务（5 分钟内可完成）/ 日程（需特定时间）/ 习惯（长期重复）
4. 仅输出 JSON：`{"suggestion": "...", "type": "...", "suggested_time": "..."}`
