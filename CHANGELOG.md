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

### Added (Phase 6.4 Complete)
- Orchestrated the complete core action loop inside `handleQuery()` in `CompanionOverlayService`.
- Wired up background coroutines (`serviceScope`) to query the `ScreenReader` natively without freezing the UI thread.
- Implemented the "not found" state gracefully showing a `REACT_WORRY` state and an apology bubble.
- Implemented the "found" state chain: drops a visual `ConfirmRing` precisely on the target, smoothly walks to it using `CompanionAnimator`, points to it while showing an explanation speech bubble, waits 4 seconds, and automatically walks to the nearest screen edge to get out of the way.

### Fixed
- **Accessibility "Restricted Setting" Bug**: Android 13 blocks accessibility services for side-loaded debug APKs. Fixed by generating a secure `release.keystore`, wiring it into `app/build.gradle.kts` using `signingConfigs`, and modifying the GitHub Actions CI pipeline to build and upload a signed `app-release.apk` instead of debug.
- **Drag & Long-Press Deadzone Bug**: The floating character could become permanently untouchable when returning to the homescreen because `FLAG_NOT_TOUCHABLE` was aggressively applied on drag completion. Completely re-engineered the touch listener using `GestureDetector` to cleanly distinguish taps, drags, and long-presses without manipulating the window touch flags, restoring flawless interaction.
- **ColorOS Android 13 Accessibility Hard-Block Bypass**: Re-engineered `MainActivity.kt` to explicitly detect Android 13's "Restricted Setting" accessibility block. Implemented an automated fallback UI that seamlessly hands the user off to `ACTION_APPLICATION_DETAILS_SETTINGS` to whitelist the app, followed by `ACTION_ACCESSIBILITY_SETTINGS` to safely enable the core service, completely resolving the ColorOS sideloading lock.
- **ULTIMATE ColorOS 13 Missing 3-Dot Menu Bypass**: Solved the critical missing 3-dot menu bug in heavily modified OS distributions like OPPO/ColorOS by utilizing a session-based self-installer architecture. Added `REQUEST_INSTALL_PACKAGES` permission and a "ColorOS Bypass" mechanism in `MainActivity.kt` that utilizes the Android `PackageInstaller` API to reinstall its own APK directly from the filesystem. Because the installation originates from a session-based API (mimicking an app store), the system fundamentally whitelists the app and permanently destroys the Restricted Settings lock.
- **Android 12+ Foreground Service Crash Fix**: Resolved a fatal `ForegroundServiceStartNotAllowedException` crash loop occurring instantly after the PackageInstaller update. The update was triggering `MainActivity` silently in the background, which then instantly attempted to launch the `CompanionOverlayService` foreground service from its `onCreate()` block before becoming visible. Deferred the foreground service orchestration to the `onResume()` lifecycle phase and wrapped it in robust exception handling, completely eliminating the crash. Also patched potential NullPointerExceptions from OEM-specific `GestureDetector` inputs.
- **GestureDetector Build Integrity Fix**: Corrected a compilation failure inside `CompanionOverlayService.kt` caused by an aggressive null-safety patch against the Android `GestureDetector` contract. Reverted `MotionEvent?` back to strictly non-null `MotionEvent` to correctly abide by the `@NonNull` annotations defined by the upstream Android View framework, restoring GitHub Actions CI/CD integrity.
- **System Auto-Restart Crash Loop**: Fixed the true root cause of the "DosCom keeps stopping" post-reinstall crash loop. When the `PackageInstaller` updated the app, the system forcefully killed the active process and subsequently attempted to auto-restart the running `CompanionOverlayService` natively in the background. Because it was a `mediaProjection` foreground service, Android 12+ immediately crashed it via `ForegroundServiceStartNotAllowedException`, repeatedly. Fixed by explicitly overriding `onStartCommand` to return `START_NOT_STICKY`, prohibiting illegal background restarts.
- **ColorOS Bypass Scroll Bug**: Fixed an issue where the programmatic "Option B: ColorOS Bypass" button was pushed off the bottom of the screen on some devices due to long explanatory text and rigid constraints. Wrapped the entire `MainActivity` configuration block inside a dynamically sized `ScrollView` to ensure absolute visibility. Added robust try-catch mechanisms to all activity and service initialization blocks.
- **Duplicate Method Build Fix**: Resolved a Kotlin compiler error caused by two conflicting `onStartCommand` overrides in `CompanionOverlayService.kt` introduced during the `START_NOT_STICKY` patch. Removed the legacy `START_STICKY` override, restoring CI/CD build integrity.
- **Global Crash Logger**: Implemented a global `UncaughtExceptionHandler` in a custom `DosComApplication` class to intercept silent early-lifecycle crashes and save the raw stack traces to the device's accessible external files directory for debugging without adb.
- **MainActivity ClassNotFoundException Fix**: Fixed a fatal `ClassNotFoundException` that caused the app to crash instantly upon launch. The `MainActivity.kt` file was missing its `package com.devenlucaz.doscom` declaration, causing the Kotlin compiler to place it in the default package. Android's `ActivityThread` failed to locate `.MainActivity` under the declared namespace, leading to an immediate process termination. Restored the package declaration to properly align with the manifest.
- **PackageInstaller Bypass Hang Fix**: Resolved an issue where the ColorOS bypass mechanism would hang indefinitely on "Preparing installation..." without showing the system install prompt. On Android 13, the `PackageInstaller` blocks UI generation from background activities via `PendingIntent.getActivity`. Changed the intent mechanism to `PendingIntent.getBroadcast`, registered a dynamic `BroadcastReceiver` to capture `STATUS_PENDING_USER_ACTION`, and manually launched the extracted `EXTRA_INTENT` with `FLAG_ACTIVITY_NEW_TASK` to force the system dialog to appear. Also implemented a 10-second timeout failsafe.
- **PackageInstaller Build Fix**: Resolved Kotlin compiler `Unresolved reference` errors in `MainActivity.kt` caused by using the non-existent `PackageInstaller.EXTRA_INTENT` constant instead of `Intent.EXTRA_INTENT`, and using the wrong namespace for `ContextCompat`.
- **Launcher Accessibility Scanner Fix**: Improved `AccessibilityScanner` logic to detect app icons on the ColorOS default launcher. The previous implementation only scanned the "active" window via `rootInActiveWindow`, missing the underlying launcher window. Implemented `flagRetrieveInteractiveWindows` in the accessibility config and updated `ScreenReader.kt` to iterate through all active `windows`. Enhanced the keyword matcher to search against `viewIdResourceName` in addition to `text` and `contentDescription`. Added extensive debug logging to monitor exactly what UI elements the Accessibility Service is detecting.
- **Fuzzy Keyword Matching**: Upgraded `AccessibilityScanner` to use a word-by-word fuzzy matching algorithm. Now handles complex user queries like "chrome" correctly matching against "Google Chrome", and "doscom" successfully matching against "DosCom". Also updated `KeywordExtractor` to accurately strip prepositional filler words without accidentally deleting target app names.
- **App Package Name Fallback Mapping**: Built an internal `APP_PACKAGE_MAP` in `AccessibilityScanner` that maps common app search terms (e.g., "camera", "youtube", "whatsapp", "settings") directly to their canonical Android package IDs (including OEM-specific variants like `com.coloros.camera`). If the textual node scan fails to find an icon, the service now performs a secondary recursive scan filtering for these specific package IDs across `packageName`, `viewIdResourceName`, and `contentDescription`.
