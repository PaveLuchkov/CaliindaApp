package com.lpavs.caliinda.core.data.suggestions

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/** Список чипов ⇄ JSON для DataStore. Битые записи пропускаем, а не роняем весь список. */
object SuggestionChipsCodec {
  fun encode(chips: List<SuggestionChip>): String =
      JSONArray(
              chips.map { chip ->
                JSONObject().apply {
                  put("id", chip.id)
                  put("builtIn", chip.builtIn)
                  chip.shortName?.let { put("short", it) }
                  chip.fullName?.let { put("full", it) }
                  chip.window?.let {
                    put("from", it.from.toString())
                    put("to", it.to.toString())
                  }
                }
              })
          .toString()

  fun decode(json: String): List<SuggestionChip>? =
      runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
              runCatching {
                    val o = array.getJSONObject(i)
                    val from = o.optString("from").takeIf { it.isNotEmpty() }
                    val to = o.optString("to").takeIf { it.isNotEmpty() }
                    SuggestionChip(
                        id = o.getString("id"),
                        builtIn = o.optBoolean("builtIn"),
                        shortName = o.optString("short").takeIf { o.has("short") },
                        fullName = o.optString("full").takeIf { o.has("full") },
                        window =
                            if (from != null && to != null) {
                              HourWindow(LocalTime.parse(from), LocalTime.parse(to))
                            } else null)
                  }
                  .getOrNull()
            }
          }
          .getOrNull()
}
