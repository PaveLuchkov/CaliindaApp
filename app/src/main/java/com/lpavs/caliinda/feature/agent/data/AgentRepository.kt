package com.lpavs.caliinda.feature.agent.data

import android.util.Log
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.remote.agent.UserContext
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.remote.agent.AgentRemoteDataSource // Используем переименованный класс
import com.lpavs.caliinda.core.data.remote.agent.ChatApiResponse
import com.lpavs.caliinda.core.data.remote.agent.ChatMessage
import com.lpavs.caliinda.core.data.remote.agent.MessageAuthor
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface AgentRepository {
    suspend fun sendMessage(message: String): Result<ChatMessage>
}

@Singleton
class AgentRepositoryImpl @Inject constructor(
    private val remoteDataSource: AgentRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val calendarStateHolder: ICalendarStateHolder
) : AgentRepository {

    override suspend fun sendMessage(message: String): Result<ChatMessage> {
        val timeZoneId = settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }

        val userContext = UserContext(
            timezone = timeZoneId,
            timezoneOffset = ZoneId.of(timeZoneId).rules.getOffset(java.time.Instant.now()).toString(),
            glanceDate = calendarStateHolder.currentVisibleDate.value.toString(),
            language = Locale.getDefault().language
        )

        val apiResult = remoteDataSource.runChat(message, userContext)

        return apiResult.map { apiResponse ->
            parseApiResponse(apiResponse)
        }
    }

    private fun parseApiResponse(apiResponse: ChatApiResponse): ChatMessage {
        val responsePayload = apiResponse.response

        return when (responsePayload) {
            is String -> {
                ChatMessage(
                    text = responsePayload,
                    author = MessageAuthor.AGENT
                )
            }
            is Map<*, *> -> {
                try {
                    val messageData = responsePayload["message"] as Map<*, *>
                    val text = messageData["message"] as String
                    val suggestions = messageData["suggestions"] as? List<String> ?: emptyList()

                    ChatMessage(
                        text = text,
                        author = MessageAuthor.AGENT,
                        suggestions = suggestions
                    )
                } catch (e: Exception) {
                    Log.e("AgentRepositoryImpl", "Failed to parse structured response", e)
                    ChatMessage(
                        text = "Ошибка при обработке ответа.",
                        author = MessageAuthor.AGENT
                    )
                }
            }
            else -> {
                ChatMessage(
                    text = "Неподдерживаемый формат ответа.",
                    author = MessageAuthor.AGENT
                )
            }
        }
    }
}