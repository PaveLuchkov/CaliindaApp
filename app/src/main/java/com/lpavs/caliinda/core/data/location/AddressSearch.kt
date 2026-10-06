package com.lpavs.caliinda.core.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.lpavs.caliinda.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Подсказки адресов через системный геокодер: бесплатно и без ключей (на устройствах с
 * сервисами Google). Адреса и города находит хорошо, названия заведений — хуже.
 */
@Singleton
class AddressSearch
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  val isAvailable: Boolean
    get() = Geocoder.isPresent()

  suspend fun suggest(query: String, max: Int = 5): List<String> {
    if (!isAvailable || query.isBlank()) return emptyList()
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses =
        runCatching {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                  geocoder.getFromLocationName(
                      query,
                      max,
                      object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) =
                            cont.resume(addresses)

                        override fun onError(errorMessage: String?) = cont.resume(emptyList())
                      })
                }
              } else {
                // До API 33 — только блокирующий вызов, поэтому в фоне.
                withContext(ioDispatcher) {
                  @Suppress("DEPRECATION") geocoder.getFromLocationName(query, max).orEmpty()
                }
              }
            }
            .getOrDefault(emptyList())
    return addresses.mapNotNull { it.label() }.distinct()
  }

  /**
   * Строка для поля места: «улица, дом, город». Первая строка адреса у заведений бывает
   * с описанием («нотариальная контора, только выдача…»), поэтому собираем сами; её берём,
   * только если улицы нет — например, искали город.
   */
  private fun Address.label(): String? {
    // Номер дома иногда приходит только в featureName («28») — берём, если это номер.
    val house = subThoroughfare ?: featureName?.takeIf { it.firstOrNull()?.isDigit() == true }
    val street = thoroughfare?.let { listOfNotNull(it, house).joinToString(", ") }
    val short = listOfNotNull(street, locality ?: adminArea).joinToString(", ")
    return short.takeIf { street != null } ?: getAddressLine(0)?.takeIf { it.isNotBlank() } ?: short.ifBlank { null }
  }
}
