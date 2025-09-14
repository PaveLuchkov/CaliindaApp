package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable sealed interface AgentResponsePayload

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
data class ChatApiResponse(val agent: String, val response: Any)

@Serializable
data class ResponseMessage(val message: String, val suggestions: List<String> = emptyList())

@Serializable
data class StructuredResponse(
    val previews: Map<String, PreviewType>? = null,
    val message: ResponseMessage
)

@Serializable
@SerialName("days_plan")
data class DaysPlanResponse(val summary: String, val days: List<DayPlan>) : AgentResponsePayload

@Serializable
@SerialName("suggestion_plan")
data class SuggestionPlanResponse(
    val summary: String,
    val suggestions: List<Suggestion>,
    @SerialName("general_advice") val generalAdvice: GeneralAdvice? = null
) : AgentResponsePayload

data class EventPreview(val action: PreviewAction, val eventIds: List<String>)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val author: String,
    val suggestions: List<String> = emptyList(),
    val previews: List<EventPreview> = emptyList()
)

@Serializable
data class ScheduledEvent(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val title: String,
    val description: String? = null
)

@Serializable data class DayPlan(val date: String, val schedule: List<ScheduledEvent>)

@Serializable
enum class Weekday {
  Monday,
  Tuesday,
  Wednesday,
  Thursday,
  Friday,
  Saturday,
  Sunday
}

@Serializable
data class Slot(
    val day: Weekday,
    val name: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

@Serializable data class GeneralAdvice(val title: String, val text: String)

@Serializable
data class Suggestion(
    val title: String,
    val description: String,
    @SerialName("is_recommended") val isRecommended: Boolean = false,
    val slots: List<Slot>
)
