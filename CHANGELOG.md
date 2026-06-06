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

### Added (Phase 4.4 Complete)
- Created `CoordinateMapper` utility class for converting raw screen targets into clamped layout parameters for the character view.
- Implemented `fromNodeCoords()` to ensure the character centers precisely on interactive targets without drifting off-screen.
- Implemented `fromPercent()` to translate vision model percentage coordinates (e.g., Gemini Vision bounding boxes) into real pixel locations.

### Added (Phase 5.1 Complete)
- Created `CompanionAnimator` utility object for orchestrating smooth character movements across the screen.
- Implemented `walkTo()` function utilizing a `ValueAnimator` to dynamically translate the view, scaling the animation duration relative to distance (400ms - 2000ms).
- Added automatic visual directional flipping (`scaleX`) and walk state assignment (`WALK_LEFT`, `WALK_RIGHT`) based on target coordinates.
- Implemented `walkToEdge()` helper function to automate returning the companion to the nearest screen edge.

### Added (Phase 5.2 & 5.3 Complete)
- Created `ConfirmRing` custom view to draw an animated pulsing circle (scaling and fading) at specific target coordinates to visually confirm automated clicks.
- Added `showConfirmRing` method in `CompanionOverlayService` to instantiate and display the ring over the target before automatically destroying itself.
- Created `SpeechBubble` view to display AI explanation text inside a rounded-rectangle bubble with fade-in and auto-dismiss (4-second) fade-out mechanics.
- Added `showSpeechBubble` method in `CompanionOverlayService` that intelligently positions the text above or below the companion based on vertical screen position to avoid clipping.

### Added (Phase 5.4 Complete)
- Created `ScreenReader` utility class acting as the primary brain for parsing the screen.
- Implemented `findTarget()` function featuring a two-layer fallback architecture.
- **Layer 1**: Uses `KeywordExtractor` and `AccessibilityScanner` for lightning-fast, native UI tree matching, mapping raw coordinates via `CoordinateMapper`.
- **Layer 2**: Created a `GeminiVisionClient` stub to automatically fall back to Vision AI analysis (using `fromPercent` mapping) if the native scan fails.

### Added (Phase 6.1 Complete)
- Created `ScreenshotHelper` utility wrapper for Android's `MediaProjection` API.
- Implemented `requestPermission` and `onPermissionResult` to securely acquire and cache screen capture permissions.
- Implemented asynchronous `captureScreen` function utilizing `ImageReader` and `VirtualDisplay` to capture screen frames without blocking the main thread.
- Automatically crops stride padding and resizes raw bitmaps to a maximum of 800px width.
- Implemented `bitmapToBase64()` helper to optimize payloads with JPEG 70% compression and Base64 string encoding for Vision API readiness.

### Added (Phase 6.2 Complete)
- Added Long-Press gesture detection directly to the companion's floating UI touch handler (`CompanionOverlayService`).
- Implemented 500ms delay threshold with a 10px slop movement cancellation to differentiate from drag gestures.
- Upon successful long-press: triggers 50ms haptic vibration, sets character to `LISTEN` state, and immediately fires `ScreenshotHelper.captureScreen()`.
- Captures the exact screen state *before* the chat input UI expands, caching it directly into `lastScreenshot`.
- Added 300ms transition delay before exposing the `showChatInput()` mechanism, while dropping `FLAG_NOT_FOCUSABLE` & `FLAG_NOT_TOUCHABLE` from WindowManager to permit keyboard input.

### Added (Phase 6.3 Complete)
- Created `ChatInputOverlay` view that dynamically injects a rounded-card chat interface at the bottom of the screen.
- Implemented smooth 250ms slide-up and slide-down `ObjectAnimator` transitions for the UI.
- Implemented `onQuerySubmitted()` callback to securely pass the user's raw text query alongside the perfectly timed `lastScreenshot` back to the service.
- Wired up keyboard auto-focus on spawn and keyboard hide on dismiss.
- Replaced the `showChatInput()` stub in `CompanionOverlayService` to orchestrate the overlay spawn and safely restore `WindowManager` flags upon closure.


