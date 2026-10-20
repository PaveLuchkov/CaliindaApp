package com.lpavs.caliinda.core.data.utils

/**
 * Описание для чтения: Outlook и Teams кладут туда HTML со стилями — показываем из него текст.
 * Только для показа: само описание не трогаем, иначе правка в Caliinda переписала бы его у всех.
 * Свой разбор, а не Html.fromHtml, — чтобы проверялся юнит-тестами на JVM.
 */
fun readableDescription(raw: String): String {
  val text = if (looksLikeHtml(raw)) htmlToText(raw) else raw
  return text
      .lines()
      // Линии-разделители приглашений («____», «-::~:~::~-») — шум.
      .filterNot { it.trim().length >= MIN_SEPARATOR && SEPARATOR.matches(it.trim()) }
      .joinToString("\n") { it.trimEnd() }
      .replace(BLANK_LINES, "\n\n")
      .trim()
}

private fun looksLikeHtml(text: String): Boolean = HTML_HINT.containsMatchIn(text)

private fun htmlToText(html: String): String =
    html
        // Невидимое содержимое — целиком, вместе с тегами.
        .replace(Regex("<(style|script|head)\\b[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        // Переносы в исходнике HTML ничего не значат — значат теги.
        .replace(Regex("\\s*\\n\\s*"), " ")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|tr|li|h[1-6]|table)>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "• ")
        .replace(Regex("<[^>]+>"), "")
        .let(::decodeEntities)
        .lines()
        .joinToString("\n") { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }

private fun decodeEntities(text: String): String =
    text
        .replace(Regex("&#(\\d+);")) { m ->
          m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m ->
          m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        // &amp; — последним: иначе «&amp;lt;» превратился бы в «<».
        .replace("&amp;", "&")

private val HTML_HINT =
    Regex("<(html|body|div|p|br|span|table|font|style|a)\\b|&nbsp;", RegexOption.IGNORE_CASE)
private val SEPARATOR = Regex("[_\\-=~:.*]+")
private const val MIN_SEPARATOR = 10
private val BLANK_LINES = Regex("\\n{3,}")
