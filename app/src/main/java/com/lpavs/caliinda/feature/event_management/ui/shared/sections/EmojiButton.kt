package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.foundation.background
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
import com.lpavs.caliinda.core.data.suggestions.leadingEmoji

/**
 * Кнопка эмодзи слева в поле названия. Показывает эмодзи, уже стоящее в начале, или значок;
 * по тапу — панель: подходящие к названию и свои из истории календаря.
 */
@Composable
fun EmojiButton(
    title: String,
    suggestions: EmojiSuggestions,
    onPick: (String) -> Unit,
    enabled: Boolean,
) {
  var open by rememberSaveable { mutableStateOf(false) }
  val current = leadingEmoji(title)
  Box {
    IconButton(onClick = { open = true }, enabled = enabled) {
      if (current != null) Text(current, fontSize = 22.sp)
      else
          Icon(
              Icons.Rounded.AddReaction,
              contentDescription = stringResource(R.string.emoji_button))
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
                  EmojiSection(stringResource(R.string.emoji_matching), suggestions.matching, current, pick)
                }
                EmojiSection(
                    stringResource(
                        if (suggestions.fromHistory) R.string.emoji_yours else R.string.emoji_popular),
                    // Подходящие уже показаны выше — не дублируем.
                    suggestions.yours - suggestions.matching.toSet(),
                    current,
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
    current: String?,
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
                    // Стоящее сейчас в названии — на подложке: здесь заливка и значит «выбрано».
                    .then(
                        if (emoji == current) Modifier.background(colorScheme.secondaryContainer)
                        else Modifier)
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
