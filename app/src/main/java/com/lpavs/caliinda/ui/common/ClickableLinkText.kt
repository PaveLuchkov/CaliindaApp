package com.lpavs.caliinda.ui.common

import android.util.Log
import androidx.compose.foundation.text.ClickableText // Используем M3 ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun ClickableLinkText(
    text: String,
    modifier: Modifier = Modifier,
    uriHandler: UriHandler = LocalUriHandler.current
) {
    val linkRegex = remember { Regex("\\[([^]]+)]\\(([^)]+)\\)") } // [Text](URL)
    val primaryColor = colorScheme.primary

    val annotatedString = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            linkRegex.findAll(text).forEach { matchResult ->
                val linkText = matchResult.groupValues[1]
                val url = matchResult.groupValues[2]
                val startIndex = matchResult.range.first
                val endIndex = matchResult.range.last + 1

                // Добавляем текст до ссылки
                append(text.substring(lastIndex, startIndex))

                // Добавляем аннотацию и стилизованный текст ссылки
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline)
                ) {
                    append(linkText)
                }
                pop() // Снимаем аннотацию и стиль

                lastIndex = endIndex
            }
            // Добавляем оставшийся текст после последней ссылки
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = LocalTextStyle.current.copy(color = LocalContentColor.current), // Наследуем стиль и цвет
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        Log.e("ClickableLinkText", "Failed to open URI ${annotation.item}", e)
                        // Можно показать Toast об ошибке открытия ссылки
                    }
                }
        }
    )
}