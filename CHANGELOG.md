# Changelog

## [1.0.0] - 2026-06-06
### Added (Phase 1 Complete)
- Created the foundational empty Android project skeleton.
- Set up Gradle build files (Kotlin DSL) with minSdk 26 and targetSdk 33.
- Added basic `MainActivity.kt` and `activity_main.xml`.
- Configured GitHub Actions CI workflow to build an APK on pushes to the `main` branch.
- Declared all required permissions in `AndroidManifest.xml` (SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, etc.).
- Defined initial foreground and accessibility service declarations (`CompanionOverlayService`, `DosComAccessibilityService`).
- Created `accessibility_service_config.xml` for screen reading capabilities.
- Fixed AndroidX compatibility by enabling `android.useAndroidX` and `android.enableJetifier` in `gradle.properties`.

### Added (Phase 2.1 Complete)
- Created `CompanionOverlayService` as a Foreground Service shell with persistent low-priority notification.
- Added `ServiceManager` utility with functions to start and stop the overlay service safely across Android versions.

### Added (Phase 2.2 Complete)
- Added `WindowManager` overlay to `CompanionOverlayService`.
- Created an 80x80dp colored placeholder view floating at initial coordinates (x=50, y=300).
- Configured layout parameters with `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE`, and `FLAG_NOT_TOUCHABLE`.
- Wired the overlay view to be added upon service creation and removed upon destruction.

### Added (Phase 2.3 & 2.4 Complete)
- Implemented `ScreenMetrics` utility class to easily access screen bounds and system bar dimensions.
- Added drag-and-drop mechanics to the overlay view via an `OnTouchListener`.
- Enabled screen edge clamping during drags.
- Implemented edge snapping animation on touch release using `ValueAnimator`.

