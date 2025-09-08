package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.Serializable

@Serializable(with = PreviewTypeSerializer::class)
sealed class PreviewType {
  @Serializable data class Search(val search: List<String>) : PreviewType()

  @Serializable data class Update(val update: List<String>) : PreviewType()

  @Serializable data class Create(val create: List<String>) : PreviewType()

  @Serializable data class Delete(val delete: List<String>) : PreviewType()
}

enum class PreviewAction {
  SEARCH,
  UPDATE,
  CREATE,
  DELETE
}

@Serializable(with = ChatApiResponseSerializer::class)
data class ChatApiResponse(
    val agent: String,
    val response: Any
)

@Serializable
data class ResponseMessage(val message: String, val suggestions: List<String> = emptyList())

@Serializable
data class StructuredResponse(
    val previews: Map<String, PreviewType>? = null,
    val message: ResponseMessage
)

@Serializable
@SerialName("days_plan")
data class DaysPlanResponse(
    val summary: String,
    val days: List<DayPlan>
) : AgentResponsePayload

@Serializable
@SerialName("suggestion_plan")
data class SuggestionPlanResponse(
    val summary: String,
    val suggestions: List<Suggestion>,
    @SerialName("general_advice")
    val generalAdvice: GeneralAdvice? = null
) : AgentResponsePayload

data class EventPreview(val action: PreviewAction, val eventIds: List<String>)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val author: String,
    val suggestions: List<String> = emptyList(),
    val previews: List<EventPreview> = emptyList()
)
