package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun <T : Any> BaseEventList(
    items: List<T>,
    key: (T) -> Any,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T, Boolean, () -> Unit) -> Unit
) {
    var expandedId by remember { mutableStateOf<Any?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        headerContent?.let {
            item { it() }
        }

        items(items = items, key = key) { item ->
            val itemId = key(item)
            val fadeSpringSpec =
                spring<Float>(
                    dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            val sliderSpringSpec =
                spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMediumLow)
            val popUndUpSpec =
                spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow)

            AnimatedVisibility(
                visible = true,
                enter =
                    slideInVertically(
                        initialOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
                exit =
                    fadeOut(animationSpec = fadeSpringSpec) +
                            slideOutVertically(
                                targetOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
                modifier =
                    Modifier.animateItem(
                        placementSpec = popUndUpSpec,
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessHigh))) {
                itemContent(
                    item,
                    expandedId == itemId,
                    { expandedId = if (expandedId == itemId) null else itemId }
                )
            }
        }
    }
}