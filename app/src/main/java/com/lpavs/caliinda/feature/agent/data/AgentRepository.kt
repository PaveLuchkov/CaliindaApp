package com.lpavs.caliinda.feature.agent.data

import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.remote.agent.UserContext
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.remote.agent.AgentRemoteDataSource // Используем переименованный класс
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface AgentRepository {
    suspend fun sendMessage(message: String): Result<Unit> // Можно изменить <Unit> на модель ответа, если она появится
}

@Singleton
class AgentRepositoryImpl @Inject constructor(
    private val remoteDataSource: AgentRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val calendarStateHolder: ICalendarStateHolder
) : AgentRepository {

    override suspend fun sendMessage(message: String): Result<Unit> {
        val timeZoneId = settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }

        val userContext = UserContext(
            timezone = timeZoneId,
            timezoneOffset = ZoneId.of(timeZoneId).rules.getOffset(java.time.Instant.now()).toString(),
            glanceDate = calendarStateHolder.currentVisibleDate.value.toString(),
            language = Locale.getDefault().language
        )

        return remoteDataSource.run_chat(message, userContext)
    }
}