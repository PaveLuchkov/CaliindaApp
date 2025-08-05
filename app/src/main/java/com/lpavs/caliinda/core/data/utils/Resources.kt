package com.lpavs.caliinda.core.data.utils
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    data class StringResource(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText()

    data class PluralsResource(
        @PluralsRes val resId: Int,
        val quantity: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText()

    object Empty : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (formatArgs.isEmpty()) {
                    context.getString(resId)
                } else {
                    context.getString(resId, *formatArgs.toTypedArray())
                }
            }
            is PluralsResource -> {
                if (formatArgs.isEmpty()) {
                    context.resources.getQuantityString(resId, quantity)
                } else {
                    context.resources.getQuantityString(resId, quantity, *formatArgs.toTypedArray())
                }
            }
            is Empty -> ""
        }
    }

    companion object {
        fun from(text: String): UiText = DynamicString(text)

        fun from(@StringRes resId: Int, vararg args: Any): UiText =
            StringResource(resId, args.toList())

        fun fromPlurals(@PluralsRes resId: Int, quantity: Int, vararg args: Any): UiText =
            PluralsResource(resId, quantity, args.toList())

        fun empty(): UiText = Empty
    }
}