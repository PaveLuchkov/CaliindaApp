package com.lpavs.caliinda.util

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
  Toast.makeText(this, message, duration).show()
}
