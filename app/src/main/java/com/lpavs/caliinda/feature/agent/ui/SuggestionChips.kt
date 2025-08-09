package com.lpavs.caliinda.feature.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.ui.theme.cuid


@Composable
fun SuggestionChipsRow(
    chips: List<String>,
    enabled: Boolean,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = cuid.padding),
        horizontalArrangement = Arrangement.spacedBy(space = cuid.padding, alignment = Alignment.CenterHorizontally),
        contentPadding = PaddingValues(cuid.padding)) {
        items(chips) { chip ->
            SuggestionChip(
                onClick = { /* onChipClick(chip) */ },
                label = { Text(chip) },
                modifier = Modifier.height(35.dp),
                enabled = enabled)
        }
    }
}