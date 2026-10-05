package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import android.content.Context
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip
import com.lpavs.caliinda.core.ui.util.fullText
import com.lpavs.caliinda.core.ui.util.shortText
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips

fun SuggestionChip.toUi(context: Context) =
    SugNameChips(key = id, name = shortText(context), fullText = fullText(context))
