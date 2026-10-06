package com.example.myhermes.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    HERMES,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: String = "Sekarang",
    val thoughtProcess: String? = null,
    val isStreaming: Boolean = false,
    val isLiked: Boolean? = null
)
