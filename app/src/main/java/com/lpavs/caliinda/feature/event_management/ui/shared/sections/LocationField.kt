@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.AppMotion

/**
 * Поле места с подсказками адресов от геокодера. Подсказки — пока печатают; выбрали адрес —
 * прячутся, свой текст тоже годится (геокодер не знает многих заведений).
 */
@Composable
fun LocationField(
    location: String,
    onLocationChange: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
    viewModel: LocationSuggestionsViewModel = hiltViewModel(),
) {
  val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
  // Подсказки только для набранного руками: после выбора адреса или открытия формы — нет.
  var typed by rememberSaveable { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    DetailField(
        value = location,
        onValueChange = {
          typed = true
          onLocationChange(it)
          viewModel.onQueryChange(it)
        },
        label = stringResource(R.string.location),
        icon = Icons.Rounded.Place,
        enabled = enabled,
        focusRequester = focusRequester,
        singleLine = true)
    AnimatedVisibility(
        visible = typed && suggestions.isNotEmpty(),
        enter = fadeIn(AppMotion.fastEffectsSpec()) + expandVertically(AppMotion.defaultSpatialSpec()),
        exit = fadeOut(AppMotion.fastEffectsSpec()) + shrinkVertically(AppMotion.defaultSpatialSpec())) {
          Column(
              verticalArrangement = Arrangement.spacedBy(2.dp),
              modifier = Modifier.padding(top = 2.dp)) {
                suggestions.take(MAX_SHOWN).forEach { address ->
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .clip(shapes.small)
                              .background(colorScheme.surfaceContainerHigh)
                              .clickable(enabled = enabled) {
                                typed = false
                                onLocationChange(address)
                              }
                              .padding(horizontal = 16.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            address,
                            style = typography.bodyMedium,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                      }
                }
              }
        }
  }
}

private const val MAX_SHOWN = 4
