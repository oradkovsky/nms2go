# nms2go

Android app to collect supplier price lists from Gmail, parse Excel attachments, and send orders.

## What it does
- **Senders (Configuration)**: store suppliers (`SenderEntity` via Room) - company, email, receiver, parser type. Add/edit/remove, share via QR (`QrScanScreen`, `QrUtils`).
- **Overview**: loads latest Gmail message per sender (`GmailApiClient` + `GmailRepository`), shows `SenderOverview` status (`FOUND`/`NO_MESSAGES`/`NO_ATTACHMENTS`). Triggers Gmail OAuth (`Google Identity` + `AuthorizationRequest` with `gmail.readonly` / `gmail.send`).
- **Compare & Order (Parsed)**: parses Excel (`ExcelParser` + `Apache POI`, `EmailDateParser` for `Date` header), shows `ParsedScreen` with filter, sort by price, pagination (`PAGE_SIZE=20` in `UiConstants`), quantity stepper (`MAX_QUANTITY=9999`). State hoisted to `ParsedViewModel` (`ParsedUiState.Idle/Loading/Content`).
- **Review**: `ReviewViewModel` aggregates chosen rows (qty > 0), sends grouped order emails per receiver (`GmailApiClient.sendMessage` with HTML table), records to `OrderHistoryRepository`/`OrderDao`.
- **Orders History**: `OrdersHistoryScreen` / `OrderDetailScreen` from Room.
- **Navigation**: `Nms2GoApp` `NavHost` (`OVERVIEW`, `CONFIG`, `QR_SCAN`, `PARSED`, `REVIEW`, `ORDERS`) with `ModalNavigationDrawer`, `FirebaseCrashlytics` logging.

## Tech stack
- **Language/build**: Kotlin 2.0.21, AGP 8.13.2, KSP, `compileSdk 36` / `minSdk 26`, Java 11, desugaring
- **UI**: Jetpack Compose BOM 2026.06, Material3, Navigation Compose 2.9.8, Activity Compose
- **DI / DB**: Hilt 2.51.1 (`@AndroidEntryPoint`, `@HiltViewModel`), Room 2.8.4 (KSP), `hilt-navigation-compose`
- **Async**: `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, coroutines (`IoDispatcher`)
- **Integrations**: Google Play Services Auth (`play-services-auth` 21.6.0), CameraX + ML Kit barcode, ZXing, Firebase BOM / Crashlytics, Apache POI 5.3.0

## Project structure
```
app/src/main/java/com/ror/nms2go/
  MainActivity.kt          # @AndroidEntryPoint, viewModels, Gmail OAuth via Identity
  Nms2GoApplication.kt     # @HiltAndroidApp
  ExcelParser.kt / EmailDateParser.kt / GmailAttachmentUtils.kt
  data/                    # Room (AppDatabase, SenderDao, OrderDao), GmailApiClient, repositories
  ui/                      # Nms2GoApp (NavHost), Overview/Parsed/Review/Orders screens + ViewModels, theme
  di/                      # DatabaseModule, DispatcherModule
```

## Setup
1. Firebase: add `google-services.json` (package `com.ror.nms2go` / `...debug`).
2. Google Cloud: enable Gmail API, create Android OAuth client for `com.ror.nms2go`, add test users on consent screen.
3. `local.properties` with `sdk.dir`, optional `keystore.properties` for release signing.
4. Sync: `./gradlew :app:assembleDebug`

## Build & run
```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:installDebug            # device/emulator
./gradlew :app:assembleRelease         # release (requires keystore.properties + version.properties)
```

## Tests
```bash
./gradlew :app:testDebugUnitTest              # JUnit (ExcelParser, GmailAttachmentUtils, etc.)
./gradlew :app:connectedDebugAndroidTest      # Espresso/Compose (OrderSendFlow, OverviewNavigation, ParsedBulkDate, SentOrders)
./gradlew :app:connectedDebugAndroidTest -PvisualDelay=true # with visual delay
```
Test runner `androidx.test.runner.AndroidJUnitRunner`, `BuildConfig.VISUAL_TEST_DELAY` controls `TestVisuals` delays.

## Versioning
Git tags `x.y.z` are the single source of truth (`app/build.gradle.kts` resolves via `git describe`).
`versionCode = major*10000 + minor*100 + patch`, `versionName` is the exact tag on release commits
(e.g. `1.0.7`) or `<tag>-<distance>-g<sha>[-dirty]` on dev builds (fallback `0.0.0-dev` when no tag is reachable).
Release a new version: `git tag 1.0.7 && git push origin 1.0.7`, then `./gradlew :app:assembleRelease`
(release builds fail on untagged HEAD by design).
