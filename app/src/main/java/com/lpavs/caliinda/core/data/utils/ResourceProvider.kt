package com.lpavs.caliinda.core.data.utils

import androidx.annotation.StringRes

sealed class ResourceProvider {
    data class DynamicString(val value: String) : ResourceProvider()
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any>
    ) : ResourceProvider()
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args.toTypedArray()
            )
        }
    }
}
