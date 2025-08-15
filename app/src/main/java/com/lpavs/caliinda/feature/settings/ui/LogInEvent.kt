package com.lpavs.caliinda.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.RoundedPolygonShape
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.ui.components.events.calculateShapeContainerSize
import com.lpavs.caliinda.feature.calendar.ui.components.events.lerpOkLab

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