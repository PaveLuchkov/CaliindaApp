package com.lpavs.caliinda.core.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.lpavs.caliinda.R

/**
 * Начертания RobotoFlex. Объявлены один раз, чтобы не собирать FontFamily заново на каждой
 * рекомпозиции карточек.
 */
@OptIn(ExperimentalTextApi::class)
object CaliindaFonts {
  private fun robotoFlex(vararg settings: FontVariation.Setting) =
      FontFamily(
          Font(R.font.robotoflex_variable, variationSettings = FontVariation.Settings(*settings)))

  /** Заголовок «сегодня», управление. */
  val Heavy = robotoFlex(FontVariation.weight(750))

  /** Заголовок обычного дня. */
  val SemiBold = robotoFlex(FontVariation.weight(600))

  /** Основной текст карточек. */
  val Card = robotoFlex(FontVariation.weight(600), FontVariation.width(100f))

  /** Текущее событие: узкое, наклонное. */
  val CardCurrent =
      robotoFlex(
          FontVariation.weight(600),
          FontVariation.grade(70),
          FontVariation.width(65f),
          FontVariation.slant(-5f))

  /** Текущий «проект»: узкое, жирное. */
  val ProjectCurrent = robotoFlex(FontVariation.weight(800), FontVariation.width(60f))
}
