package com.lpavs.caliinda.core.data.remote.agent

// Модель для всего ответа от API
data class ChatApiResponse(
    val agent: String,
    val response: Any // Используем Any, так как тип может быть String или Map
)

// Модели для случая, когда response - это объект
data class StructuredMessage(
    val message: String,
    val suggestions: List<String>? = null
)

data class StructuredResponse(
    val previews: Map<String, Any>? = null, // Можно сделать более строгую типизацию, если структура previews известна
    val message: StructuredMessage
)

enum class MessageAuthor {
    USER, AGENT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(), // Уникальный ID для списков в Jetpack Compose
    val text: String,
    val author: MessageAuthor,
    val suggestions: List<String> = emptyList()
)