# Caliinda

Android-календарь на Jetpack Compose (Kotlin, Hilt). Данные — напрямую из системного
`CalendarContract` (Google Calendar синхронизирует туда сам), без бэкенда и OAuth.

- **Дизайн:** Material 3 Expressive, динамические цвета, `MaterialShapes`. Правила и чек-лист —
  `docs/DESIGN.md`. Любой новый UI сверять с ним.
- **План работ:** `docs/ROADMAP.md`. Идём по этапам, отмечаем выполненное.
- **Промо-ролики:** `promo/` (Remotion). Принципы — что передать и как — в `promo/README.md`.

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
- **Ченджлог ведём постепенно.** Каждое заметное для пользователя изменение сразу дописываем
  в ченджлог текущей готовящейся версии — в том же мерже в main. Готовящаяся версия — следующая
  после последнего тега (`git describe --tags --abbrev=0`): пока пользователь не скажет «нужна
  новая версия», всё копим под ней. Файлы — по одному на язык в
  `docs/changelog/<версия>/<локаль>.txt` (en-US, ru-RU, de-DE, es-ES, fr-FR, hi-IN — как в
  `docs/store-listing.md`) и `play-console.txt` — все языки в тегах `<en-US>…</en-US>`, чтобы
  вставить в «Что нового» Play одним разом. Пишем для пользователя, а не про код: что стало
  удобнее; похожие пункты объединяем; последний пункт — «исправлены ошибки и улучшена
  производительность»; лимит Play — 500 символов на язык, проверять длину.
- **Новая версия** (только когда пользователь скажет): сверить ченджлог с
  `git log vX.YY..HEAD --first-parent`, `versionCode` +1, `versionName` — `Breakfast — 2.NN`
  (`app/build.gradle.kts`), коммит версии в main, тег `vX.YY` на нём. Следующие изменения
  копятся уже под 2.NN+1.
- Скиллы Google в `.claude/skills` (edge-to-edge, agp-9-upgrade, testing-setup) — общие
  советы; при расхождении с `docs/DESIGN.md` и этими соглашениями правила проекта главнее.
