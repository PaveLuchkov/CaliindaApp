package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid

/**
 * Кнопка сохранения формы. Стоит внизу шторки и поднимается над клавиатурой, чтобы быть под
 * большим пальцем и не сдвигать форму, когда появляется.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaveBar(enabled: Boolean, isLoading: Boolean, onSave: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onSave,
      enabled = enabled && !isLoading,
      modifier =
          modifier
              .imePadding()
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = cuid.ContainerPadding)
              .height(ButtonDefaults.MediumContainerHeight),
      shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
      contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)) {
        if (isLoading) {
          LoadingIndicator(
              color = colorScheme.onPrimary,
              modifier = Modifier.size(ButtonDefaults.iconSizeFor(30.dp)))
        } else {
          Text(
              text = stringResource(R.string.save),
              style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
        }
      }
}
