package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import com.lpavs.caliinda.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@ExperimentalMaterial3ExpressiveApi
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    onCreateEventClick: () -> Unit,
) {
  HorizontalFloatingToolbar(
      modifier = modifier,
      expanded = true,
      content = {
        IconButton(onClick = onCreateEventClick) {
          Icon(imageVector = Icons.Filled.AddCircle, contentDescription = stringResource(R.string.create_event))
        }
      },
  )
}
