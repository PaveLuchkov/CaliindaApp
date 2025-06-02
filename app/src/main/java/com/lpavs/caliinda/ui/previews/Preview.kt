import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@OptIn(ExperimentalMaterial3ExpressiveApi::class) // Для ButtonGroup
@Composable
fun IconAndTextButtonGroupScreen() {
    var isFavorite by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                FilledIconButton(
                    onClick = {
                        if (menuState.isExpanded) {
                            menuState.dismiss()
                        } else {
                            menuState.show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Localized description"
                    )
                }
            },
            modifier = Modifier,
            expandedRatio = ButtonGroupDefaults.ExpandedRatio,
            horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement,
            {
                clickableItem(
                    onClick = { println("Edit clicked") },
                    label = "",
                    icon = { Icon(Icons.Filled.Edit, contentDescription = "") },
                    enabled = true // Можно опустить, т.к. true по умолчанию
                )
                // 2. Использование toggleableItem
                toggleableItem(
                    checked = isFavorite,
                    label = "",
                    onCheckedChange = { newCheckedState -> isFavorite = newCheckedState },
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle Favorite"
                        )
                    }
                )
                // 3. Использование customItem
            }
        )
    }
}



@Preview(showBackground = true)
@Composable
fun IconAndTextButtonGroupScreenPreview() {
        IconAndTextButtonGroupScreen()
}
