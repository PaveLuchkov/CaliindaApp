package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import com.lpavs.caliinda.core.ui.theme.CaliindaFonts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.Typography

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun ManagementTitle(
) {
  val headerBackgroundColor = colorScheme.tertiary
  val headerTextColor = colorScheme.onTertiary
  val headerTextStyle = Typography.titleLargeEmphasized

  val headerFontFamily =
      CaliindaFonts.Heavy

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clip(RoundedCornerShape(25.dp))
              .background(color = headerBackgroundColor)) {
        Text(
            text = "Regular events",
            style = headerTextStyle,
            fontFamily = headerFontFamily,
            color = headerTextColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
      }
}
