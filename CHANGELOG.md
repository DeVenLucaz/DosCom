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

### Fixed (Overlay Launch Bug)
- Fixed `MainActivity.kt` so that it correctly checks for `Settings.canDrawOverlays` permission on app launch.
- If granted, it automatically calls `ServiceManager.startOverlayService()`.
- If not granted, it correctly redirects the user to the `ACTION_MANAGE_OVERLAY_PERMISSION` settings screen.

### Added (Phase 3.1 Complete)
- Created `CharacterState` enum for handling robot states (IDLE, WALK, POINT, REACT, etc.).
- Created `CompanionRenderer` custom View to draw the character using Canvas API.
- Replaced the purple placeholder view in `CompanionOverlayService` with the new `CompanionRenderer`.
- Added logic for tracking state frames to prepare for future sprite animation.

### Added (Phase 3.2 & 3.3 Complete)
- Added an animation loop using `Handler` and `Runnable` in `CompanionRenderer` running at 150ms intervals.
- Implemented `IDLE_BOB` logic with a sine-wave Y-offset to make the robot float dynamically.
- Implemented `IDLE_BLINK` logic that draws closed eyes on specific frames.
- Implemented a random idle behavior scheduler in `CompanionOverlayService` that transitions between idle states every 3-8 seconds.
- Added pupil-shifting logic for `IDLE_LOOK_LEFT` and `IDLE_LOOK_RIGHT` states.

### Added (Phase 4.1 Complete)
- Created `DosComAccessibilityService` extending Android's `AccessibilityService`.
- Added a companion object with a static `instance` and `isConnected()` method for global state checking.
- Verified service is correctly registered in `AndroidManifest.xml` with proper intent filters and permissions.

### Added (Phase 4.2 & 4.3 Complete)
- Created `AccessibilityScanner` utility for traversing the Android accessibility node tree to find UI elements by matching text or `contentDescription`.
- Implemented node center coordinate calculation using `getBoundsInScreen` for precise interaction targets.
- Created `KeywordExtractor` utility that automatically removes common natural language filler words (e.g., "how", "do", "i", "tap") from user queries to isolate specific UI targets.


