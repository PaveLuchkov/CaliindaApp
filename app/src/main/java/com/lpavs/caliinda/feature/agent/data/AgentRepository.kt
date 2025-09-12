package com.lpavs.caliinda.feature.agent.data

import android.util.Log
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.remote.agent.AgentRemoteDataSource
import com.lpavs.caliinda.core.data.remote.agent.ChatApiResponse
import com.lpavs.caliinda.core.data.remote.agent.DaysPlanResponse
import com.lpavs.caliinda.core.data.remote.agent.EventPreview
import com.lpavs.caliinda.core.data.remote.agent.PreviewAction
import com.lpavs.caliinda.core.data.remote.agent.PreviewType
import com.lpavs.caliinda.core.data.remote.agent.StructuredResponse
import com.lpavs.caliinda.core.data.remote.agent.SuggestionPlanResponse
import com.lpavs.caliinda.core.data.remote.agent.UserContext
import com.lpavs.caliinda.core.data.remote.agent.domain.AgentResponseContent
import com.lpavs.caliinda.core.data.remote.agent.domain.DaysPlanContent
import com.lpavs.caliinda.core.data.remote.agent.domain.ErrorResponse
import com.lpavs.caliinda.core.data.remote.agent.domain.SuggestionPlan
import com.lpavs.caliinda.core.data.remote.agent.domain.TextMessageResponse
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface AgentRepository {
  suspend fun sendMessage(message: String): Result<AgentResponseContent>

  suspend fun deleteSession(): Result<Unit>
}

@Singleton
class AgentRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: AgentRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val calendarStateHolder: ICalendarStateHolder
) : AgentRepository {

  override suspend fun sendMessage(message: String): Result<AgentResponseContent> {
    val timeZoneId = settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }

    val userContext =
        UserContext(
            timezone = timeZoneId,
            timezoneOffset =
                ZoneId.of(timeZoneId).rules.getOffset(java.time.Instant.now()).toString(),
            glanceDate = calendarStateHolder.currentVisibleDate.value.toString(),
            language = Locale.getDefault().language)

    val apiResult = remoteDataSource.runChat(message, userContext)

    return apiResult.map { apiResponse -> parseApiResponse(apiResponse) }
  }

  override suspend fun deleteSession(): Result<Unit> {
    val result = remoteDataSource.deleteChat()
    return result
  }

  private fun parseApiResponse(apiResponse: ChatApiResponse): AgentResponseContent {
    val payload = apiResponse.response

    return when (payload) {
      is String -> {
        TextMessageResponse(
            mainText = payload,
            suggestions = emptyList(),
            highlightedEventInfo = emptyMap(),
            author = apiResponse.agent)
      }
      is StructuredResponse -> {
        val infoMap = buildMap {
          payload.previews?.forEach { (_, previewType) ->
            val eventPreview =
                when (previewType) {
                  is PreviewType.Search -> EventPreview(PreviewAction.SEARCH, previewType.search)
                  is PreviewType.Update -> EventPreview(PreviewAction.UPDATE, previewType.update)
                  is PreviewType.Create -> EventPreview(PreviewAction.CREATE, previewType.create)
                  is PreviewType.Delete -> EventPreview(PreviewAction.DELETE, previewType.delete)
                }
            eventPreview.eventIds.forEach { id -> put(id, eventPreview.action) }
          }
        }
        TextMessageResponse(
            mainText = payload.message.message,
            suggestions = payload.message.suggestions,
            highlightedEventInfo = infoMap,
            author = apiResponse.agent)
      }
      is DaysPlanResponse -> {
        DaysPlanContent(mainText = payload.summary, suggestions = emptyList(), days = payload.days)
      }
      is SuggestionPlanResponse -> {
        SuggestionPlan(
            mainText = payload.summary,
            suggestions = emptyList(),
            suggestionItems = payload.suggestions,
            generalAdvice = payload.generalAdvice)
      }
      else -> {
        Log.w(
            "AgentRepositoryImpl",
            "Received an unexpected response type: ${payload::class.java.name}")
        ErrorResponse(mainText = "Неподдерживаемый формат ответа от сервера.")
      }
    }
  }
}
