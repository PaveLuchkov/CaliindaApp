package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun LogInEvent(
    onSignInClick: () -> Unit,
)
{
    val darkerShadowColor = Color.Black
    val cardElevation = cuid.CurrentEventElevation
    val cardFontFamily =
        FontFamily(
            Font(
                R.font.robotoflex_variable,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(600),
                        FontVariation.width(100f),
                    )))
    val cardBackground = colorScheme.tertiaryFixed
    val cardTextColor = colorScheme.onTertiaryFixedVariant
    val cardBorderColor = colorScheme.onSurface
    val cardHeight = 100.dp
    val textStyle = Typography.headlineSmall

    Box(
        modifier =
            Modifier
                .padding(
                horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                vertical = CalendarUiDefaults.ItemVerticalPadding)
                .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
                .shadow(
                    elevation = cardElevation,
                    shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                    clip = false,
                    ambientColor = darkerShadowColor,
                    spotColor = darkerShadowColor)
                .border(
                    width = 2.dp,
                    color = cardBorderColor,
                    shape = RoundedCornerShape(cuid.EventItemCornerRadius))
                .background(cardBackground)
                .height(cardHeight)) {
        Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier.weight(1f) // Занимает все место, ОСТАВЛЯЯ место для кнопок снизу
                    .fillMaxWidth()
                    .padding(
                        horizontal = cuid.ItemHorizontalPadding,
                        vertical = cuid.StandardItemContentVerticalPadding),
            contentAlignment = Alignment.TopStart) {

            Column(verticalArrangement = Arrangement.Top) {
                Text(
                    text = "Log in with Google",
                    color = cardTextColor,
                    style = textStyle,
                    fontFamily = cardFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = cuid.ItemHorizontalPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onSignInClick() },
                    contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Log in")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Log in")
                }
            }
        }
    }
}