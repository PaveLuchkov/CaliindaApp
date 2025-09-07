package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
data class ChatApiResponse(
    val agent: String,
    @Serializable(with = ChatResponseSerializer::class)
    val response: Any
)

@Serializable
data class ResponseMessage(
    val message: String,
    val suggestions: List<String> = emptyList()
)

@Serializable
data class StructuredResponse(
    val previews: JsonObject? = null,
    val message: ResponseMessage
)

enum class MessageAuthor {
    USER, AGENT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val author: MessageAuthor,
    val suggestions: List<String> = emptyList()
)