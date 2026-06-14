package com.lemon.veil.ai

data class StepItem(
    val suggestion: String = "",
    val location: String? = null,
    val suggested_time: String? = null
)

data class TaskItem(
    val suggestion: String = "",
    val type: String = "任务",
    val location: String? = null,
    val suggested_time: String? = null,
    val steps: List<StepItem>? = null
)

data class SuggestionResponse(
    val mode: String = "tasks",
    val message: String? = null,
    val options: List<String>? = null,
    val tasks: List<TaskItem>? = null
)
