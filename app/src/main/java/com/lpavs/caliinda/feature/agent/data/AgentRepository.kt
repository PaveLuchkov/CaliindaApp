package com.lpavs.caliinda.feature.agent.data

// класс
import android.util.Log
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.remote.agent.AgentRemoteDataSource
import com.lpavs.caliinda.core.data.remote.agent.ChatApiResponse
import com.lpavs.caliinda.core.data.remote.agent.ChatMessage
import com.lpavs.caliinda.core.data.remote.agent.EventPreview
import com.lpavs.caliinda.core.data.remote.agent.PreviewAction
import com.lpavs.caliinda.core.data.remote.agent.PreviewType
import com.lpavs.caliinda.core.data.remote.agent.StructuredResponse
import com.lpavs.caliinda.core.data.remote.agent.UserContext
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface AgentRepository {
  suspend fun sendMessage(message: String): Result<ChatMessage>

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

  override suspend fun sendMessage(message: String): Result<ChatMessage> {
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

  private fun parseApiResponse(apiResponse: ChatApiResponse): ChatMessage {
    val responsePayload = apiResponse.response

    return when (responsePayload) {
      is String -> {
        ChatMessage(text = responsePayload, author = apiResponse.agent)
      }

      is StructuredResponse -> {
        // НОВАЯ ЛОГИКА ЗДЕСЬ
        val eventPreviews =
            responsePayload.previews?.mapNotNull { (_, previewType) ->
              // Трансформируем PreviewType в наш EventPreview
              when (previewType) {
                is PreviewType.Search -> EventPreview(PreviewAction.SEARCH, previewType.search)

                is PreviewType.Update -> EventPreview(PreviewAction.UPDATE, previewType.update)

                is PreviewType.Create -> EventPreview(PreviewAction.CREATE, previewType.create)

                is PreviewType.Delete -> EventPreview(PreviewAction.DELETE, previewType.delete)
              }
            } ?: emptyList()

        ChatMessage(
            text = responsePayload.message.message,
            author = apiResponse.agent,
            suggestions = responsePayload.message.suggestions,
            previews = eventPreviews)
      }

      else -> {
        Log.w(
            "AgentRepositoryImpl",
            "Received an unexpected response type: ${responsePayload::class.java.name}")
        ChatMessage(
            text = "Неподдерживаемый формат ответа (внутренняя ошибка).",
            author = apiResponse.agent)
      }
    }
  }
}
