package com.lemon.veil.ai

import android.content.Context
import com.google.gson.Gson
import com.lemon.veil.utils.safeApiCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface AiRepository {
    suspend fun getSuggestion(messages: List<ChatMessage>, context: Context): Result<SuggestionResponse>
}

class AiRepositoryImpl : AiRepository {

    private val gson = Gson()

    override suspend fun getSuggestion(messages: List<ChatMessage>, context: Context): Result<SuggestionResponse> {
        val result = safeApiCall {
            ApiClient.deepSeekApi.getSuggestion(
                ChatRequest(
                    model = "deepseek-chat",
                    messages = listOf(
                        ChatMessage("system", getPrompt(context))
                    ) + messages,
                    temperature = 0.3,
                    response_format = mapOf("type" to "json_object")
                )
            )
        }

        return result.map { response ->
            val rawContent = response.choices.firstOrNull()?.message?.content ?: ""
            val jsonMatch = Regex("""\{[\s\S]*\}""").find(rawContent)
                ?: return result.map { SuggestionResponse(mode = "chat", message = "解析失败") }

            gson.fromJson(jsonMatch.value, SuggestionResponse::class.java)
        }
    }

    private fun getPrompt(context: Context): String {
        val now = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
        return """
            你是一个行动规划助手。当前时间：$now。
            你和用户正在进行多轮对话，帮助用户将想法转化为行动任务。
            对话历史已附在上下文中，请参考前文理解用户意图。

            每次回复必须输出 JSON，格式：
            {
              "mode": "chat" 或 "tasks",
              "message": "回复文本，纯文字，不要加图标或 emoji",
              "options": ["选项1", "选项2"] 或 null,
              "tasks": [
                {
                  "suggestion": "纯文字，不要图标",
                  "type": "任务",
                  "location": "地点或null",
                  "suggested_time": "yyyy-MM-dd HH:mm 或 null",
                  "steps": [
                    {"suggestion": "子步骤1", "location": "地点或null", "suggested_time": "yyyy-MM-dd HH:mm 或 null"},
                    {"suggestion": "子步骤2", "location": "地点或null", "suggested_time": "yyyy-MM-dd HH:mm 或 null"}
                  ] 或 null
                }
              ] 或 null
            }

            mode 说明：
            - chat: 用户输入还不够具体，继续对话引导，可附带 options（2-4 个）
            - tasks: 已明确，直接生成任务列表

            规则：
            1.【最重要】优先合并为 task + steps：
               - 多个动作只要服务于同一事务（同一趟出门、同一项目、同一件事的准备+执行），必须合并为 1 个 task + steps，禁止拆成多个独立 task
               - 只有动作确实彼此无关（如"买牛奶"和"还书"是两个不相关的事），才各自作为独立 task
               - 不确定时强制合并，宁可多 step 也不要多 task
            2. message 和 suggestion 必须纯文字，禁止任何图标、emoji、符号表情
            3. chat 模式下可给出 2-4 个 options 供用户点击
            4. tasks 模式必须给出完整 tasks 数组
            5. 结合当前时间推断 suggested_time，格式严格为 yyyy-MM-dd HH:mm
            6. location 提取具体地点，没有则填 null
            7. 仅输出 JSON，不要包含 Markdown 或其他文本
        """.trimIndent()
    }

    companion object {
        private val _instance by lazy { AiRepositoryImpl() }
        fun getInstance(): AiRepository = _instance
    }
}
