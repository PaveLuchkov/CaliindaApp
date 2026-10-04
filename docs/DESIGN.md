# Дизайн-система Caliinda

Caliinda — минималистичный, необычный и красивый календарь для быстрого взгляда на день и проекты.
Основа — **Material 3 Expressive** от Google. Своё поверх M3 добавляем только там, где это
характер приложения (звёзды на карточках, высота по длительности), и строим это из токенов M3.

## Принципы

1. **Сначала M3 Expressive.** Прежде чем рисовать свой компонент, ищем готовый в
   `androidx.compose.material3` (FloatingToolbar, LoadingIndicator, ButtonGroup, SplitButton,
   FAB Menu, expressive-типографика `*Emphasized`, `MaterialShapes`). Свой компонент — только
   если готового нет.
2. **Вертикальный экран — главный.** Время идёт сверху вниз. Основные действия — в нижней
   трети экрана, под большим пальцем. Горизонталь — для переключения режимов (проекты ⇄ день).
3. **Взгляд за секунду.** Важное считывается без чтения текста: высота = длительность,
   цвет tertiary = «сейчас», форма = характер события. Текста минимум.
4. **Минимализм формы ввода.** Название и время — сразу, остальное — по запросу.
5. **Движение осмысленное.** Пружины, а не линейные tween; анимация объясняет изменение
   (куда ушла карточка, откуда пришла форма).

## Цвет

- Только роли `MaterialTheme.colorScheme` — **никаких захардкоженных `Color(0x…)`** в UI
  (исключения: `Color.Transparent`, тени).
- `ThemeMode.SYSTEM` = динамические цвета (`dynamicLight/DarkColorScheme`, minSdk 32 — доступны
  всегда). Остальные темы (Cold, Warm, Pinky, Green, Sunny) — полные M3-схемы в
  `core/ui/theme/<name>/`.
- Семантика ролей в приложении:
  - `primaryContainer` / `onPrimaryContainer` — обычное событие и проект;
  - `tertiaryContainer` / `onTertiaryContainer` — текущее событие, «сегодня»;
  - переход primary → tertiary для ближайшего события — `lerpOkLab` по `proximityRatio`;
  - `secondary` — заголовок не-сегодняшнего дня;
  - `surfaceContainer*` — поля формы, фоны секций;
  - прошлое (история проектов) — приглушать через alpha/`surfaceVariant`, не через серый hex.
- Цвет календаря из провайдера (`DISPLAY_COLOR`) — только как тонкий акцент (точка/полоска),
  карточку целиком красим ролями темы, чтобы не ломать гармонию динамических цветов.
- Каждый экран проверяем в светлой и тёмной теме и минимум в двух ThemeMode.

## Формы (shapes)

- Скругления — из `MaterialTheme.shapes` / `ShapeDefaults` или токенов `cuid`
  (`EventItemCornerRadius = 20.dp`, `ContainerCornerRadius = 25.dp`). Новые радиусы не
  выдумываем — добавляем токен в `ValDefaults.kt`.
- Декоративные формы — `MaterialShapes` (Flower, Sunny, Burst, Clover, Heart…) и
  `RoundedPolygon` из `androidx.graphics.shapes`. Форма события детерминирована его `id`
  (`EventUiModelMapper.generateShapeParams`) — одно событие всегда выглядит одинаково.
- Морфинг форм (`Morph`) — для смены состояний (нажатие, «сейчас» ↔ «потом»,
  загрузка), а не просто для красоты.
- Иконочные кнопки — `IconButtonDefaults.small*Shape` / `smallContainerSize` (как в AppBar).

## Типографика

- Шрифт — RobotoFlex (variable), начертания объявлены один раз в `CaliindaFonts`
  (`core/ui/theme/Fonts.kt`). Новые начертания добавлять туда, не создавать `FontFamily`
  в composable.
- Стили — `Typography` (M3) + expressive-варианты `*Emphasized` для акцента («сегодня»,
  текущее событие). Ось ширины/наклона RobotoFlex — способ выделить «сейчас» без смены цвета.

## Движение

- Предпочтительно `MaterialTheme.motionScheme` (expressive) и `spring(...)`.
  Сейчас `CaliindaTheme` использует `MaterialTheme`, а не `MaterialExpressiveTheme` —
  при переходе получим expressive motion scheme по умолчанию (см. ROADMAP).
- Списки: `eventItemAnimation()` из `BaseEventList.kt` (пружинная перестановка/появление).
- Хаптика: `ContextClick` — тап по карточке, `LongPress` — долгое нажатие,
  `TextHandleMove` — лёгкие переключения. Не вибрировать на каждый скролл.

## Компоненты и раскладка

- Нижняя панель — `HorizontalFloatingToolbar` с отступом `FloatingToolbarDefaults.ScreenOffset`.
- Модальные формы — `ModalBottomSheet`; кнопка подтверждения — снизу, над клавиатурой
  (`imePadding`), а не сверху.
- Загрузка — `LoadingIndicator` (expressive), не `CircularProgressIndicator`.
- Подтверждения разрушительных действий — снекбар с «Отменить» вместо диалога, где возможно.
- Отступы — токены `cuid` (`padding = 8.dp`, `ItemHorizontalPadding = 16.dp`).

## Тексты

- Все пользовательские строки — в `res/values*/strings.xml` (en, ru, de, es, fr, hi).
  Никаких строк в коде, включая `contentDescription`. Множественные числа — `plurals`.

## Чек-лист для нового UI

- [ ] Есть готовый M3 Expressive компонент? Используем его.
- [ ] Только роли `colorScheme`, проверено в light/dark и SYSTEM + одна кастомная тема.
- [ ] Радиусы и отступы из токенов.
- [ ] Главное действие под большим пальцем.
- [ ] Анимации — пружины / motionScheme.
- [ ] Строки и plurals в ресурсах, `contentDescription` заполнен.
- [ ] Preview добавлен.
