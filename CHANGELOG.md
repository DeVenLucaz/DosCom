# Changelog

## [2.3.0] - 2026-06-09
### Fixed
- Fixed Full Ghost mode becoming inescapable by adding a "Disable Ghost Mode" action button directly to the foreground notification that appears immediately when the mode is active and correctly resets touch state to interactive.

## [2.2.0] - 2026-06-09
### Fixed
- Replaced subtle shake reaction with a full judgmental hang pose animation (`TimeReactionEngine`, `CompanionOverlayService`).
- Reduced idle sub-animation schedule delay to 8-15 seconds for better visibility (`IdleAnimationEngine`).

## [2.1.0] - 2026-06-09
### Fixed
- Fixed mascot size slider distorting character body shape when resized by applying scale uniformly to both axes in `CompanionRenderer.kt`.
- Fixed reaction button animations; positive reactions now visibly jump and show blush for 1500ms, negative reactions dim the antenna and show a sad mouth for 1500ms.

## [2.0.0] - 2026-06-08
### Added (V2 Phase 16 Complete — Final Polish)
- Made `CompanionOverlayService` return `START_STICKY` for better process persistence.
- Added quick action buttons to the foreground notification (Settings, Ghost Mode, Stop).
- Unified animation speed globally, pushing the user's `anim_speed` preference down into `IdleAnimationEngine`, `MimeEngine`, and `ClimbEngine`.
- Added ColorOS specific accessibility hints to `OnboardingActivity`.
- Added automatic Crash Recovery. DosCom will gracefully apologize with an "oops 😅" and shrug animation if the app crashed previously.
- Implemented and enabled ProGuard rules protecting the behavior engines.
- Finalized version string to `2.0.0`.

## [2.0.0-alpha.16] - 2026-06-08
### Added (V2 Phase 15 Complete — Onboarding V2)
- Overhauled `OnboardingActivity` to use a step-by-step sequential layout designed with programmatic UI and glassmorphism styling.
- Included mode selection built into onboarding. Permissions now adapt depending on the selected mode (Awake mode needs API Key, Aware mode needs Accessibility).
- Replaced the control panel in `MainActivity` with direct passthrough to the `CompanionOverlayService`, relying entirely on `SettingsActivity` as the new control interface.
- Configured dynamic mode polling to ensure permissions like OVERLAY and ACCESSIBILITY are validated before proceeding.

## [2.0.0-alpha.15] - 2026-06-08
### Added (V2 Phase 14 Complete — Touch Modes and Resize)
- Created `TouchModeManager` to wrap `SharedPreferences` for interaction modes (Interactive, Semi-Ghost, Full-Ghost).
- Refactored `CompanionOverlayService` touch listener to properly handle custom double-tap (mode switch) and triple-tap (open settings) logic without relying on third-party gesture detectors.
- Implemented Pinch-to-Resize on the robot overlay to dynamically adjust the scale, calculating the distance between two pointers, mapping it to the configured `mascot_scale` limits.
- Set up `FLAG_NOT_TOUCHABLE` handling for ghost modes, and added a separate `semiGhostView` overlay target layer to wake the character upon sustained holding.

## [2.0.0-alpha.14] - 2026-06-08
### Added (V2 Phase 13 Complete — Aware Mode Extra Sense)
- Updated `ScreenReader` to build full accessibility screen context dumps.
- Modified `GeminiVisionClient` to inject the real-time screen context into the system prompt and process base64-encoded screenshots for visual awareness.
- Updated `CompanionOverlayService` to handle "can you see" queries, extracting screenshots and sending them to the Vision API.
- Implemented `RepeatDetector` to passively monitor accessibility events.
- Wired `DosComAccessibilityService` to trigger a curious reaction ("psst... you good? 👀") on repetitive UI interactions, opening chat upon user tap.
- Updated `walkTo` logic to display a confused shake animation when requested elements cannot be found on the screen.

## [2.0.0-alpha.13] - 2026-06-08
### Added (V2 Phase 12 Complete — Awake Mode Gemini Voice)
- Created `ConversationHistory` to maintain a rolling window of the last 10 conversational exchanges for context.
- Updated `GeminiVisionClient` with a new `speak()` function to inject personality, user mood, app context, and chat history into the LLM system instructions.
- Patched `CompanionOverlayService` to intelligently intercept user queries during AWAKE mode, forwarding them to Gemini and rendering the returned conversational text inside the speech bubble.
- Rewarded the DosCombrain system positively each time the user interacts via chat in AWAKE mode.


## [2.0.0-alpha.12] - 2026-06-07
### Added (V2 Phase 11 Complete — Glassmorphism UI)
- Rewrote `ChatInputOverlay` with a custom translucent glass bar, custom SVG icon buttons, and sliding animations.
- Upgraded `SpeechBubble` with a frosted glass aesthetic and directional tail based on on-screen position.
- Updated `ConfirmRing` visual aesthetic with glass strokes and semi-transparent fill.
- Polished `SettingsActivity` with glass effect cards and live `CompanionRenderer` previews for each mode.

## [2.0.0-alpha.11] - 2026-06-07
### Added (V2 Phase 10 Complete — Birthday System)
- Created `BirthdaySystem` to store and retrieve the user's birthdate and DosCom's install date using persistent storage.
- Integrated an unlock listener in `TimeReactionEngine` to trigger contextual animations upon device wake.
- Built day-phase transitions for user birthdays (Midnight confetti, Morning Gift Box, Afternoon Tiny Cake, Evening Party Hat).
- Added DosCom's birthday animations where DosCom eats its own cake and gets excited/blushes when tapped.
- Rewarded the `DosCombrain` network with positive reinforcement on DosCom birthday interactions.

## [2.0.0-alpha.10] - 2026-06-07
### Added (V2 Phase 9 Complete — Emotional Memory & Reactions)
- Created `EmotionalMemory` system to persist sentiment scores based on user reactions over time.
- Implemented `MoodEngine` to adjust animation speeds and character state based on user's current mood.
- Built `ReactionBox` UI component displaying interactive glassmorphism reaction buttons on long-press.
- Wired ReactionBox to reward/penalize `DosCombrain` network and update emotional memory accordingly.
- Updated `BrainInput` to leverage actual persistent sentiment values from `EmotionalMemory`.

## [2.0.0-alpha.9] - 2026-06-07
### Added (V2 Phase 8 Complete — DosCom Brain SNN)
- Implemented `LIFCore` and `DosCombrain` featuring a Spiking Neural Network (Leaky Integrate-and-Fire) to generate unique behaviors and decision logic per install.
- Created `BrainInput` to map environmental data (battery, time, activity) into normalized tensor input for the network.
- Wired the Brain into `ToyBoxSystem` for toy selection, and `IdleAnimationEngine` for idle animation sequence preferences.
- Created `PersonalityGrowth` engine tracking user interactions and mapping emergent personality types.
- Updated `CompanionOverlayService` to instantiate and persist network weight updates silently via `BrainManager`.

## [2.0.0-alpha.8] - 2026-06-07
### Added (V2 Phase 7 Complete)
- Created `PhoneEventReceiver` to react to hardware broadcasts (power connected, low battery, headset plugged, screen state).
- Created `AppContextWatcher` to monitor foreground app usage and categorize apps (Music, Camera, Maps, etc.), updating DosCom's held props and activity to match.
- Created `TimeReactionEngine` integrating context-aware time responses, accelerometer physical shake/vehicle detection, keyboard peeking (via view heights), and screenshot photobombs.
- Wired all event listeners into `CompanionOverlayService` lifecycle for seamless reactive background awareness.


## [2.0.0-alpha.7] - 2026-06-07
### Added (V2 Phase 6 Complete)
- Created `ToyBoxSystem` allowing the robot to spontaneously select and interact with virtual props (Fishing Rod, Treasure Map, Book, Magnifying Glass, etc.) when bored.
- Created `DiscoverySystem` bridging `ToyBoxSystem` exploration to distinct collectible finds (e.g., Pirate Coin, Tiny Bug, Ancient Fossil).
- Created `HomeCornerSystem` to passively track position and eventually anchor the character to their favorite screen coordinate.
- Wired `ToyBoxSystem` directly into the `IdleAnimationEngine` with a 30% random-trigger chance per idle-action cycle.


## [2.0.0-alpha.6] - 2026-06-07
### Added (V2 Phase 5 Complete)
- Created `IdleAnimationEngine` to seamlessly blend animation states using continuous lerp evaluation inside a 60fps Choreographer loop.
- Introduced `ZzzParticle` sleep state, triggering automatically after a configurable idle delay with cute curl-up and Zzz visual rendering.
- Added various sub-animations (Stretch, Hiccup, Sneeze, Yawn, Coin flip, Phone check) dynamically scheduled during inactive periods.
- Replaced the deprecated `CompanionAnimator` and `CharacterState` (V1 concepts) entirely with the new declarative `AnimationState` architecture and `IdleAnimationEngine` driver.


## [2.0.0-alpha.5] - 2026-06-07
### Added (V2 Phase 4 Complete)
- Created `MovementEngine` to dynamically generate sequential AnimationState frames for 4-step climbing and crawling cycles.
- Wired dragging listeners in `CompanionOverlayService` to interpret real-time drag distance and edge proximity.
- Enabled physical synchronization between user drag speed and limb actuation (the robot now actively mimes climbing up/down side edges and crawling along top/bottom edges as it is dragged).


## [2.0.0-alpha.4] - 2026-06-07
### Added (V2 Phase 3 Complete)
- Created `PoseEngine` to calculate screen bounds and dynamically assign position-aware poses (`HANG_LEFT`, `HANG_RIGHT`, `GRIP_TOP`, `SIT_BOTTOM`, `FLOATING`).
- Implemented dragging release logic in `CompanionOverlayService` to trigger `PoseEngine`.
- Added smooth AnimationState lerping using ValueAnimator to transition from the dragging pose to the resting edge pose over 400ms.
- Handled `FLOATING` state logic by triggering a panicked spin (`bodyRotation = 360`) before walking to the nearest safe edge.


## [2.0.0-alpha.3] - 2026-06-07
### Added (V2 Phase 2b Complete)
- Created `AnimationState` data class to encapsulate all independent transformations (limbs, body position/rotation, scale) and expressions in a single reactive state object.
- Created `AnimationQueue` to manage prioritization (CRITICAL, HIGH, MEDIUM, LOW, AMBIENT) and queuing/interruption of animation sequences.
- Wired `AnimationState` directly into `CompanionRenderer`'s `onDraw` method, implementing isolated pivot points (`canvas.rotate`) for independent limb articulation.
- Registered a `Choreographer.FrameCallback` on window attach to continuously invalidate the view, creating a high-performance 60fps+ rendering loop devoid of `Thread.sleep` or traditional Handlers.


## [2.0.0-alpha.2] - 2026-06-07
### Added (V2 Phase 2a Complete)
- Created `PropType` enum to support various interactive toys and hats (Party Hat, Boombox, Detective Hat, etc.).
- Completely rewrote `CompanionRenderer` to draw the V2 Chibi robot dynamically using raw Android Canvas paths and shapes (no image assets).
- Implemented the full V2 expression system with support for pupil tracking, eye states (closed, half-closed, wide), mouth expressions (neutral, happy, worried, open), blushing, and tongue out.
- Preserved V1 animation stubs (`setState`, `nextFrame`) to prevent compilation issues until the animation engine is fully replaced in Phase 5.

## [2.0.0-alpha.1] - 2026-06-07
### Added (V2 Phase 1 Complete)
- Created the Mode System infrastructure (`CompanionMode` enum: ALIVE, AWAKE, AWARE).
- Implemented `ModeManager` to handle mode transitions and state persistence using `SharedPreferences`.
- Created the Settings shell (`SettingsActivity`) with a Glassmorphism-inspired dark layout featuring distinct sections for Mode, Appearance, Behavior, Personality, API, and About.
- Wired basic interactivity for mode selection cards and preference controls in the Settings screen.
- Updated `AndroidManifest.xml` to include `SettingsActivity`.
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
- **New App Icon**: Replaced the default black circle Android icon with a custom-designed, friendly, glowing robotic AI companion face. The icon was generated and downscaled into all proper `mipmap` densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) for a crisp appearance on any device. Fixed an issue where `AndroidManifest.xml` and `CompanionOverlayService` were still hardcoded to look for the old `drawable/ic_launcher` resources instead of the new `mipmap` variants, causing the OS to fallback to the default Android graphic.
- **Revamped README**: Completely rewrote `README.md` to be extremely user-friendly and non-technical. Added clear, emoji-rich instructions on what DosCom does, how to install it via Obtainium, how to bypass ColorOS restrictions, and how to use the overlay UI.

### Added (Phase 7 Complete)
- Implemented `VoiceInputService` wrapper utilizing Android's `SpeechRecognizer` API with `LANGUAGE_MODEL_FREE_FORM`.
- Upgraded voice recognition accuracy by forcing the `EXTRA_LANGUAGE` to "en-IN", increasing `EXTRA_MAX_RESULTS` to 3, and enabling `EXTRA_PARTIAL_RESULTS`.
- Updated `ChatInputOverlay` to include a new "Mic" button alongside the text input field.
- Implemented logic to hide the soft keyboard, enter the `CharacterState.LISTEN` animation, and trigger the voice service when the Mic button is pressed.
- Added live partial result streaming so users can watch their speech transcribe in real-time, giving them the option to correct it manually instead of auto-submitting.
- Added a pulsing red circular animation to the Mic button and a rapid blinking red animation to the companion's antenna during the active listening state.
- Handled error states gracefully by falling back to the `IDLE_BOB` state and displaying a "Couldn't hear that, try typing?" speech bubble.

### Added (Phase 8 Complete — Gemini Integration)
- Implemented full `GeminiVisionClient` with Gemini 1.5 Flash API integration for screenshot-based UI element detection (Layer 2 fallback).
- Sends base64-encoded screenshots with structured prompts; parses JSON response with `found`, `x_percent`, `y_percent`, `explanation`, and `element_description` fields.
- Added `configure(key)` and `isConfigured()` methods for API key management at runtime.
- Added markdown code fence stripping for robust response parsing.
- Created `GeminiIntentBridge` for deep-linking queries to the Gemini app with Play Store fallback.
- Updated `ScreenReader` Layer 2 to check `isConfigured()` before calling Vision API and validate the `found` flag.

### Added (Phase 9 Complete — Notification Reactions)
- Created `DosComNotificationListener` extending `NotificationListenerService` to react to incoming notifications.
- Categorizes notifications into `wave` (messages), `worry` (battery/status), and `happy` (charging/system) reactions.
- Broadcasts reaction type via `LocalBroadcastManager` to `CompanionOverlayService`.
- `CompanionOverlayService` registers a `BroadcastReceiver` that triggers character animations: `REACT_WAVE` (2s), `REACT_WORRY` with "Low battery!" speech bubble (3s), `REACT_HAPPY` (2s).
- Added `localbroadcastmanager` dependency to `build.gradle.kts`.

### Added (Phase 10 Complete — Onboarding & Polish)
- Created `OnboardingActivity` with an 8-step guided setup flow: Welcome, Overlay Permission, Accessibility Service, Screen Capture, Battery Optimization, Auto-Launch (Oppo/ColorOS), Microphone Permission, and optional Vision API Key entry.
- Each step shows an emoji icon, title, description, and an action button that turns green with a checkmark once the permission is granted.
- Created `ConfigManager` to securely store and retrieve the Gemini API key in app-private JSON storage.
- Created `BatteryOptimizationHelper` to request and check battery optimization exemption.
- Completely rewrote `MainActivity` as a minimal control panel: shows "DosCom is running" with live status indicators for Overlay, Accessibility, and Vision API, plus "Stop DosCom" and "Re-run Setup" buttons.
- `MainActivity` now routes to `OnboardingActivity` on first launch and resets onboarding if overlay permission is revoked.
