package com.lpavs.caliinda.core.data.remote.agent.domain

import com.lpavs.caliinda.core.data.remote.agent.DayPlan
import com.lpavs.caliinda.core.data.remote.agent.GeneralAdvice
import com.lpavs.caliinda.core.data.remote.agent.PreviewAction
import com.lpavs.caliinda.core.data.remote.agent.Suggestion

sealed interface AgentResponseContent {
    val mainText: String
    val suggestions: List<String>
}

data class TextMessageResponse(
    override val mainText: String,
    override val suggestions: List<String>,
    val highlightedEventInfo: Map<String, PreviewAction>
) : AgentResponseContent

data class DaysPlan(
    override val mainText: String,
    override val suggestions: List<String>,
    val days: List<DayPlan>
) : AgentResponseContent

data class SuggestionPlan(
    override val mainText: String,
    override val suggestions: List<String>,
    val suggestionItems: List<Suggestion>,
    val generalAdvice: GeneralAdvice?
) : AgentResponseContent

data class ErrorResponse(
    override val mainText: String
) : AgentResponseContent {
    override val suggestions: List<String> = emptyList()
}