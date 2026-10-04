# Caliinda

Android-календарь на Jetpack Compose (Kotlin, Hilt). Данные — напрямую из системного
`CalendarContract` (Google Calendar синхронизирует туда сам), без бэкенда и OAuth.

- **Дизайн:** Material 3 Expressive, динамические цвета, `MaterialShapes`. Правила и чек-лист —
  `docs/DESIGN.md`. Любой новый UI сверять с ним.
- **План работ:** `docs/ROADMAP.md`. Идём по этапам, отмечаем выполненное.

## Структура

- `core/data/calendar` — обёртка над CalendarContract (`CalendarProviderDataSource`).
- `core/data/repository/CalendarRepository` — чтение (день / проекты), запись, серии (RRULE/EXDATE).
- `feature/calendar` — экраны: горизонтальный пейджер [проекты | дни], вертикальный пейджер по дням.
- `feature/event_management` — форма создания/редактирования (bottom sheet).
- `core/ui/theme` — темы, `CaliindaFonts`, токены `cuid` (`ValDefaults.kt`).

## Соглашения

- Комментарии в коде — на русском, коротко, объясняют «почему».
- Пользовательские строки — только в `res/values*/strings.xml` (en, ru, de, es, fr, hi).
- M3 `DatePicker` работает в UTC — даты конвертировать через `ZoneOffset.UTC`,
  события — в поясе из настроек (`SettingsRepository.timeZoneFlow`).
- Сборка: `./gradlew assembleDebug`, тесты: `./gradlew testDebugUnitTest`.
