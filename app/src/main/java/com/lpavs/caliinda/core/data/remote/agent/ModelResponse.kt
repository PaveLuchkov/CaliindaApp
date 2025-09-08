package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.Serializable

@Serializable(with = PreviewTypeSerializer::class)
sealed class PreviewType {
    @Serializable
    data class Search(val search: List<String>) : PreviewType()

    @Serializable
    data class Update(val update: List<String>) : PreviewType()

    @Serializable
    data class Create(val create: List<String>) : PreviewType()

    @Serializable
    data class Delete(val delete: List<String>) : PreviewType()
}

enum class PreviewAction {
    SEARCH, UPDATE, CREATE, DELETE
}

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
    val previews: Map<String, PreviewType>? = null,
    val message: ResponseMessage
)

data class EventPreview(
    val action: PreviewAction,
    val eventIds: List<String>
)


data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val author: String,
    val suggestions: List<String> = emptyList(),
    val previews: List<EventPreview> = emptyList()
)