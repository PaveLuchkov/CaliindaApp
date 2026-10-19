package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.suggestions.EmojiSuggestions

/**
 * Кнопка эмодзи слева в поле названия. По тапу — панель: подходящие к названию и свои из
 * истории календаря; выбранное встаёт на место курсора ([onPick]).
 */
@Composable
fun EmojiButton(
    suggestions: EmojiSuggestions,
    onPick: (String) -> Unit,
    enabled: Boolean,
) {
  var open by rememberSaveable { mutableStateOf(false) }
  Box {
    IconButton(onClick = { open = true }, enabled = enabled) {
      Icon(Icons.Rounded.AddReaction, contentDescription = stringResource(R.string.emoji_button))
    }
    DropdownMenu(
        expanded = open,
        onDismissRequest = { open = false },
        shape = shapes.extraLarge,
        containerColor = colorScheme.surfaceContainerHigh) {
          Column(
              modifier = Modifier.width(PANEL_WIDTH).padding(horizontal = 12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val pick = { emoji: String ->
                  onPick(emoji)
                  open = false
                }
                if (suggestions.matching.isNotEmpty()) {
                  EmojiSection(stringResource(R.string.emoji_matching), suggestions.matching, pick)
                }
                EmojiSection(
                    stringResource(
                        if (suggestions.fromHistory) R.string.emoji_yours else R.string.emoji_popular),
                    // Подходящие уже показаны выше — не дублируем.
                    suggestions.yours - suggestions.matching.toSet(),
                    pick)
              }
        }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiSection(
    title: String,
    emojis: List<String>,
    onPick: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(title, style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
    FlowRow(maxItemsInEachRow = PER_ROW) {
      emojis.forEach { emoji ->
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(CELL)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onPick(emoji) }) {
              Text(emoji, fontSize = 24.sp)
            }
      }
    }
  }
}

private const val PER_ROW = 6
private val CELL = 44.dp
private val PANEL_WIDTH = CELL * PER_ROW
