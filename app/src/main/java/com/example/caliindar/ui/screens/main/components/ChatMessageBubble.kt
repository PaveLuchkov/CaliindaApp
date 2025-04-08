package com.example.caliindar.ui.screens.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import com.example.caliindar.data.model.ChatMessage // <-- Импорт
import com.example.caliindar.ui.common.ClickableLinkText // <-- Импорт

@Composable
fun ChatMessageBubble(message: ChatMessage, uriHandler: UriHandler) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            // Используем modifier.widthIn для ограничения максимальной ширины,
            // что позволяет тексту переноситься внутри бабла правильно.
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f) // Ограничиваем макс. ширину
                .clip(RoundedCornerShape(
                    topStart = if (message.isUser) 16.dp else 4.dp,
                    topEnd = if (message.isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )), // Делаем уголки разными
            color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp // Небольшая тень
        ) {
            // Используем ClickableLinkText из папки common
            ClickableLinkText(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                uriHandler = uriHandler
            )
        }
    }
}