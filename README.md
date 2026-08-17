# Клиент личного магазина приложений

Android-клиент: читает витрину `apps.json`, показывает список приложений,
скачивает APK, проверяет sha256 и ставит через системный установщик.

Kotlin · Jetpack Compose (Material 3) · minSdk 26 · targetSdk 36 · ручной DI.

## Адрес витрины

Задаётся **в одном месте** — `MANIFEST_URL` в [`gradle.properties`](gradle.properties):

```properties
MANIFEST_URL=https://raw.githubusercontent.com/<владелец>/app-store-manifest/main/apps.json
```

Оттуда значение попадает в `BuildConfig.MANIFEST_URL` и в `StoreConfig`.

## Структура

```
app/src/main/java/com/personal/appstore/
├── StoreConfig.kt          адрес витрины, интервал проверки обновлений
├── StoreApplication.kt     ServiceLocator, канал уведомлений, планирование воркера
├── data/
│   ├── ManifestParser.kt   apps.json → доменные модели, отбраковка невалидных записей
│   ├── ManifestRepository.kt  сеть + кэш, офлайн-режим со стейлом
│   ├── Sha256.kt
│   ├── local/ManifestCache.kt DataStore-кэш последнего манифеста
│   └── remote/             ManifestApi (ретраи), ApkDownloader (прогресс + sha256)
├── domain/                 статусы «Установить/Обновить/Открыть», установленные пакеты
├── installer/              PackageInstaller.Session + запасной путь через FileProvider
├── ui/                     Compose: список, детали, баннеры, ViewModel
└── worker/                 периодическая проверка обновлений + уведомления
```

## Что гарантирует клиент

- APK не передаётся установщику без совпадения sha256: файл появляется под
  финальным именем только после проверки, иначе удаляется.
- Статус считается по `versionCode` (не по строке версии).
- Без сети показывается кэш витрины с пометкой «устарело».
- Магазин публикуется в витрине как обычное приложение и обновляет сам себя
  (баннер «Обновить магазин»).

## Разрешения

| Разрешение | Зачем |
|---|---|
| `INTERNET` | витрина и APK |
| `REQUEST_INSTALL_PACKAGES` | установка APK; система один раз спросит подтверждение |
| `UPDATE_PACKAGES_WITHOUT_USER_ACTION` | 31+: обновление без лишнего диалога для своих же установок |
| `POST_NOTIFICATIONS` | 33+: уведомления о новых версиях |
| `QUERY_ALL_PACKAGES` | 30+: определить, что уже установлено (магазин личный, не для Google Play) |

## Сборка и тесты

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest     # 40 юнит-тестов
./release.sh 1.1.0 "Что нового" # публикация новой версии магазина
```
