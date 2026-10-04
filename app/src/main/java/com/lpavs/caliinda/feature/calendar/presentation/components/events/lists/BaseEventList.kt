package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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

private val itemPlacementSpec: FiniteAnimationSpec<IntOffset> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
private val itemFadeInSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
private val itemFadeOutSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessHigh)

/** Анимация появления/перестановки/исчезновения элемента списка событий. */
internal fun LazyItemScope.eventItemAnimation(): Modifier =
    Modifier.animateItem(
        placementSpec = itemPlacementSpec,
        fadeInSpec = itemFadeInSpec,
        fadeOutSpec = itemFadeOutSpec)

@Composable
fun <T : Any> BaseEventList(
    items: List<T>,
    key: (T) -> Any,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    headerContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T, Boolean, () -> Unit) -> Unit
) {
    var expandedId by remember { mutableStateOf<Any?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        userScrollEnabled = userScrollEnabled,
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        headerContent?.let {
            item { it() }
        }

        items(items = items, key = key) { item ->
            val itemId = key(item)
            Box(modifier = eventItemAnimation()) {
                itemContent(
                    item,
                    expandedId == itemId,
                    { expandedId = if (expandedId == itemId) null else itemId }
                )
            }
        }
    }
}
