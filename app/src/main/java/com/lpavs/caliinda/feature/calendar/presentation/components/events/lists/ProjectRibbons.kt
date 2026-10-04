package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts
import com.lpavs.caliinda.feature.calendar.presentation.components.page.ProjectRibbon

/** Идущие в этот день проекты — тонкие ленты с прогрессом «день N из M». */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectRibbons(ribbons: List<ProjectRibbon>, onClick: (EventDto) -> Unit) {
  if (ribbons.isEmpty()) return
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ribbons.forEach { ribbon ->
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(CircleShape)
                      .background(colorScheme.secondaryContainer)
                      .clickable { onClick(ribbon.event) }
                      .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ribbon.event.summary,
                    style = typography.labelLarge,
                    fontFamily = CaliindaFonts.Card,
                    color = colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                LinearWavyProgressIndicator(
                    progress = { ribbon.progress },
                    modifier = Modifier.width(56.dp),
                    color = colorScheme.secondary,
                    trackColor = colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                    // Волна стоит на месте: бегущая читается как загрузка, а это просто прогресс.
                    waveSpeed = 0.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.project_day_of, ribbon.dayNumber, ribbon.totalDays),
                    style = typography.labelMedium,
                    color = colorScheme.onSecondaryContainer,
                    maxLines = 1)
              }
        }
      }
}
