package com.lpavs.caliinda.core.ui.components

import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Вариант в [ChoiceDialog]. [destructive] — красится в цвет ошибки (удаление серии);
 * [selected] — текущее значение (выбор настройки), выделено и с галочкой.
 */
class ChoiceOption(
    val text: String,
    val destructive: Boolean = false,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Диалог выбора одним нажатием, как системный запрос разрешений в Android 16: иконка,
 * заголовок, пояснение и столбец раздельных кнопок — у крайних углы круглее, между
 * соседними — небольшой зазор. Последней всегда идёт «Отмена».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceDialog(
    icon: ImageVector,
    title: String,
    message: String?,
    options: List<ChoiceOption>,
    cancelText: String,
    onDismiss: () -> Unit,
) {
  val all = options + ChoiceOption(cancelText, onClick = onDismiss)
  BasicAlertDialog(
      onDismissRequest = onDismiss, modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)) {
        Surface(shape = shapes.extraLarge, color = colorScheme.surfaceContainerHigh) {
          Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(24.dp))
                Text(
                    title,
                    style = typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp))
                if (message != null) {
                  Text(
                      message,
                      style = typography.bodyMedium,
                      color = colorScheme.onSurfaceVariant,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.padding(top = 16.dp))
                }
                Spacer(Modifier.height(24.dp))
                // Вариантов может быть много (напоминания) — на маленьком экране список листается.
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())) {
                  all.forEachIndexed { index, option ->
                    val isCancel = index == all.lastIndex
                    Button(
                        onClick = option.onClick,
                        shape = stackedShape(index, all.lastIndex),
                        colors =
                            when {
                              option.selected ->
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.primary,
                                      contentColor = colorScheme.onPrimary)
                              option.destructive ->
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.errorContainer,
                                      contentColor = colorScheme.onErrorContainer)
                              isCancel ->
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.surfaceContainerHighest,
                                      contentColor = colorScheme.onSurface)
                              else ->
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.secondaryContainer,
                                      contentColor = colorScheme.onSecondaryContainer)
                            },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                          if (option.selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                          }
                          Text(option.text, textAlign = TextAlign.Center)
                        }
                  }
                }
              }
        }
      }
}

/** Скругления кнопки в столбце: крупные снаружи группы, мелкие на стыках соседей. */
@Composable
private fun stackedShape(index: Int, lastIndex: Int): CornerBasedShape {
  val outer = shapes.large.topStart
  val inner = shapes.extraSmall.topStart
  val top = if (index == 0) outer else inner
  val bottom = if (index == lastIndex) outer else inner
  return shapes.large.copy(
      topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
