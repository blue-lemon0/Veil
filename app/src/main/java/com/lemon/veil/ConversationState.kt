package com.lemon.veil

import com.lemon.veil.ai.ChatMessage
import com.lemon.veil.data.NoteEntity

data class ConversationState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reply: String? = null,
    val exploreOptions: List<String>? = null,
    val pendingTasks: List<NoteEntity> = emptyList()
) {
    val isActive: Boolean get() = messages.isNotEmpty() || isLoading
}
