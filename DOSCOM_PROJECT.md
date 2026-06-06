# 🤖 DosCom — Dosti & Companion
> *"Your AI friend that lives on your screen, sees what you see, and shows you the way"*

**Project Type:** Android Floating Assistant & Screen Companion
**Target Device:** Oppo F19 Pro+ 5G (ColorOS 13 / Android 13)
**Core Technologies:** Kotlin, Jetpack Compose, WindowManager, AccessibilityService, Gemini App Intent, Gemini Vision API (fallback)

---

## Vision

DosCom is a persistent, animated pixel-art robot-creature that floats above all your apps. When you ask it something, it doesn't just *tell* you what to tap — it **physically walks to that exact spot on your screen** and either points at it or taps it for you.

It sees your screen in two ways:
1. **AccessibilityService** — reads the UI tree to find exact element coordinates (fast, free, precise)
2. **Gemini Vision API** — looks at a screenshot when accessibility can't find the element (fallback for icon-only or canvas UIs)

AI queries go to the **Gemini app already on your phone** via Intent — no API key needed for conversation. Vision API is only used as a last resort fallback.

---

## Core Concept

```
User long-presses DosCom
        ↓
Screenshot captured instantly (screen clean, no overlay)
        ↓
300ms later → voice/text input appears
        ↓
User asks: "How do I open a project in CapCut?"
        ↓
┌─────────────────────────────────────────┐
│         DUAL SCREEN READING             │
│                                         │
│  LAYER 1: AccessibilityService          │
│  Scan UI tree for text nodes            │
│  "Projects", "Open", "New Project"      │
│  → Exact pixel coordinates returned    │
│                                         │
│  LAYER 2: Gemini Vision (fallback)      │
│  Used only if Layer 1 finds nothing     │
│  Screenshot → Gemini API → x%, y%      │
│  → Approximate coordinates returned    │
└─────────────────────────────────────────┘
        ↓
Confirm ring pulses at target (1 second)
        ↓
DosCom walks to target coordinates
        ↓
Speech bubble: "Tap here — this opens your projects"
        ↓ (optional: auto-tap mode)
AccessibilityNodeInfo.ACTION_CLICK fires
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Canvas |
| Build | Gradle + GitHub Actions (APK) |
| Overlay | `WindowManager` + `TYPE_APPLICATION_OVERLAY` |
| Screen Reading (Primary) | `AccessibilityService` + UI tree traversal |
| Screen Reading (Fallback) | Gemini Vision API + base64 screenshot |
| AI Conversation | Gemini app on device via `Intent` deep link |
| Walk Animation | `ValueAnimator` + `Choreographer.postFrameCallback` |
| Voice Input | Android `SpeechRecognizer` |
| Touch Pass-Through | `FLAG_NOT_TOUCHABLE` during idle/movement |
| Speech Bubble | Compose `Popup` overlay |

---

## Project Structure

```
doscom/
├── app/
│   └── src/main/
│       ├── java/com/devenlucaz/doscom/
│       │   ├── MainActivity.kt
│       │   ├── onboarding/
│       │   │   └── OnboardingActivity.kt         # First launch: all permissions + setup
│       │   ├── service/
│       │   │   ├── CompanionOverlayService.kt    # Core floating overlay (Foreground Service)
│       │   │   ├── DosCom AccessibilityService.kt # UI tree scanner + click executor
│       │   │   ├── ScreenCaptureService.kt       # MediaProjection (fallback screenshots)
│       │   │   └── VoiceInputService.kt          # SpeechRecognizer wrapper
│       │   ├── ai/
│       │   │   ├── GeminiIntentBridge.kt         # Fire Intent to Gemini app for conversation
│       │   │   └── GeminiVisionClient.kt         # Vision API fallback (screenshot analysis)
│       │   ├── screen/
│       │   │   ├── AccessibilityScanner.kt       # UI tree traversal, find nodes by text/desc
│       │   │   ├── CoordinateMapper.kt           # DisplayMetrics + WindowInsets coord calc
│       │   │   └── ScreenReader.kt               # Orchestrates Layer1 → Layer2 fallback
│       │   ├── character/
│       │   │   ├── CompanionRenderer.kt          # Canvas drawing + spritesheet animation
│       │   │   ├── CompanionAnimator.kt          # Walk, idle, point, react states
│       │   │   └── CharacterState.kt             # IDLE, WALKING, POINTING, TALKING, REACTING
│       │   ├── ui/
│       │   │   ├── SpeechBubble.kt               # Text popup near character
│       │   │   ├── ChatInputOverlay.kt           # Appears 300ms after screenshot
│       │   │   ├── ConfirmRing.kt                # Pulsing ring at target before walking
│       │   │   └── PermissionScreen.kt
│       │   └── utils/
│       │       ├── ScreenshotHelper.kt           # Capture + compress + base64
│       │       └── BatteryOptimizationHelper.kt  # ColorOS battery exemption
│       └── res/
│           ├── drawable/
│           │   └── companion_spritesheet.png
│           └── xml/
│               └── accessibility_service_config.xml
├── .github/
│   └── workflows/
│       └── build.yml
└── DOSCOM_PROJECT.md
```

---

## Permissions Required

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<service
    android:name=".service.DosCom AccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

---

## AccessibilityService Config

```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:notificationTimeout="100" />
```

> **Throttled by design:** DosCom only scans the UI tree when explicitly triggered (double-tap or voice command). No passive monitoring. This prevents lag and battery drain.

---

## Build Phases

### Phase 1 — The Body (P1)
**Goal:** Floating character on screen, draggable, stays above all apps.

- `CompanionOverlayService` as Foreground Service, `startForeground()` in `onCreate()`
- Character added to screen via `WindowManager`
- `OnTouchListener` for drag mechanics
- **`FLAG_NOT_TOUCHABLE`** applied during idle — user taps pass through to apps underneath
- Flag switches to touchable only when user touches DosCom directly
- Persistent low-priority notification keeps service alive
- `OnboardingActivity` handles all permissions on first launch

**Deliverable:** DosCom floats on screen, is draggable, and doesn't block app usage.

---

### Phase 2 — The Brain (P2)
**Goal:** DosCom can read the screen.

- `DosCom AccessibilityService` configured and connected
- `AccessibilityScanner` traverses UI tree on demand (triggered only, not passive)
- Finds `AccessibilityNodeInfo` objects by:
  - Text label (e.g. "Projects", "Export", "Save")
  - Content description (e.g. accessibility labels on icon buttons)
  - View ID (for known apps)
- Returns exact `Rect` bounds → `CoordinateMapper` extracts center coordinates
- `CoordinateMapper` uses `DisplayMetrics` + `WindowInsets` to account for:
  - Status bar height
  - Navigation bar height
  - Screen density

**Deliverable:** DosCom can find any labeled UI element on screen with exact pixel coordinates.

---

### Phase 3 — The Legs (P3)
**Goal:** DosCom walks to targets and can tap them.

**Walk animation (smooth 60fps):**
```kotlin
// CompanionAnimator.kt
fun walkTo(targetX: Int, targetY: Int, onArrival: () -> Unit) {
    val startX = layoutParams.x
    val startY = layoutParams.y
    val distance = hypot((targetX - startX).toFloat(), (targetY - startY).toFloat())
    val duration = (distance / screenWidth * 1800).toLong().coerceIn(400, 2000)

    ValueAnimator.ofFloat(0f, 1f).apply {
        this.duration = duration
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            val t = anim.animatedFraction
            layoutParams.x = (startX + (targetX - startX) * t).toInt()
            layoutParams.y = (startY + (targetY - startY) * t).toInt()
            // Flip sprite direction based on movement direction
            characterView.scaleX = if (targetX > startX) 1f else -1f
            windowManager.updateViewLayout(characterView, layoutParams)
        }
        doOnEnd { onArrival() }
    }.start()
}
```

**On arrival — two modes:**
- **Guide mode (default):** Play point animation + show speech bubble "Tap here — [explanation]"
- **Auto-tap mode (optional, user toggles):** Fire `AccessibilityNodeInfo.ACTION_CLICK` on the found node

**Confirm ring before walking:**
- `ConfirmRing` overlay pulses at target for 1 second before DosCom moves
- User sees if position is wrong before DosCom commits to walking there
- Tap DosCom again to cancel/retry

**Deliverable:** DosCom physically walks to UI elements and guides or taps them.

---

### Phase 4 — The Voice & Vision (P4)
**Goal:** Full interaction — voice input + Vision API fallback for elements accessibility can't find.

**Interaction flow (race-condition-free):**
1. User **long-presses** DosCom
2. Screenshot captured **immediately** — screen clean, no overlay visible yet
3. 300ms delay → chat/voice input appears
4. User speaks or types question

**Dual screen reading (ScreenReader.kt orchestration):**
```kotlin
// ScreenReader.kt
suspend fun findTarget(query: String, screenshot: Bitmap): TargetResult {

    // LAYER 1: Accessibility tree (fast, free, exact)
    val keywords = extractKeywords(query)  // NLP or simple keyword parse
    val node = accessibilityScanner.findByKeywords(keywords)
    if (node != null) {
        val coords = coordinateMapper.fromNode(node)
        return TargetResult.Found(coords, node, source = "accessibility")
    }

    // LAYER 2: Gemini Vision fallback (only if Layer 1 fails)
    val result = geminiVisionClient.analyze(screenshot, query)
    if (result.found) {
        val coords = coordinateMapper.fromPercent(result.xPercent, result.yPercent)
        return TargetResult.Found(coords, node = null, source = "vision")
    }

    return TargetResult.NotFound
}
```

**Gemini Vision prompt (used only in fallback):**
```
You are a screen navigation assistant for Android.
The user is asking: "[USER_QUESTION]"

Look at this screenshot. Identify the single most relevant UI element to tap.
Respond ONLY in this exact JSON (no markdown, no extra text):
{
  "element_description": "brief name of element",
  "x_percent": 0.0,
  "y_percent": 0.0,
  "explanation": "one sentence, max 12 words",
  "found": true
}
x_percent: 0.0 = left edge, 1.0 = right edge
y_percent: 0.0 = top edge, 1.0 = bottom edge
If nothing relevant visible, return found: false.
```

**Voice input:**
- Long-press → screenshot captured → antenna blinks (listening state)
- `SpeechRecognizer` captures short command
- Same pipeline as text input

**Deliverable:** DosCom handles any UI — labeled elements via accessibility, visual-only elements via Vision.

---

### Phase 5 — Gemini Integration (P5)
**Goal:** Connect DosCom to the Gemini app already on device. Zero API cost for conversation.

**GeminiIntentBridge.kt — fire Intent to Gemini app:**
```kotlin
// GeminiIntentBridge.kt
fun sendToGemini(context: Context, query: String) {
    // Try deep link first
    val deepLink = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://gemini.google.com/app?q=${Uri.encode(query)}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (deepLink.resolveActivity(context.packageManager) != null) {
        context.startActivity(deepLink)
        return
    }
    // Fallback: open Gemini app directly
    val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
    launch?.let { context.startActivity(it) }
}
```

**Use cases via Intent:**
- General questions → Gemini app opens with query pre-filled
- "Explain this to me" → DosCom passes context + question to Gemini
- Gemini responds in its own app window, DosCom stays floating on top

**Gemini Vision API key storage** (fallback only):
`~/.doscom/config.json` with `chmod 600`
```json
{
  "gemini_vision_api_key": "AIza..."
}
```

---

### Phase 6 — Notification Reactions (P6)
**Goal:** DosCom reacts expressively to phone events.

- `NotificationListenerService` monitors events
- Reactions (in-place animations, no walking to icon positions):
  - New message → wave animation
  - Low battery → worried animation + points toward top-right (hardcoded status bar region)
  - App crash → shrug animation
  - Alarm → jump animation
  - Charging connected → happy dance

> Walking to exact status bar icons (battery, wifi) is not possible — Android does not expose those icon positions via any API. DosCom reacts expressively in place for all notification events.

---

## Character Design

**Style:** Pixel-art robot with creature features — big expressive eyes, small rounded body, stubby legs, blinking antenna

**Animation States:**
| State | Frames | Trigger |
|---|---|---|
| `IDLE_BLINK` | 3 | Random timer 3–8s |
| `IDLE_BOB` | 4 | Continuous loop |
| `IDLE_LOOK` | 4 | Random timer |
| `WALK_RIGHT` | 6 | Moving to target (scaleX = 1) |
| `WALK_LEFT` | 6 | Moving to target (scaleX = -1, same frames) |
| `POINT` | 4 | At target, guide mode |
| `LISTEN` | 3 | Voice input active |
| `REACT_WAVE` | 5 | New notification |
| `REACT_WORRY` | 3 | Low battery |
| `REACT_HAPPY` | 4 | Charging / task complete |
| `TALK` | 2 | Speech bubble shown |

**Size:** 80×80dp — visible but not intrusive
**Edge snapping:** When idle, DosCom snaps to nearest screen edge (left or right) so center content stays clear

---

## Coordinate Mapping

```kotlin
// CoordinateMapper.kt

// From AccessibilityNodeInfo (exact)
fun fromNode(node: AccessibilityNodeInfo): Pair<Int, Int> {
    val rect = Rect()
    node.getBoundsInScreen(rect)
    return Pair(rect.centerX() - CHARACTER_WIDTH / 2,
                rect.centerY() - CHARACTER_HEIGHT / 2)
}

// From Gemini Vision percentages (fallback)
fun fromPercent(xPercent: Float, yPercent: Float): Pair<Int, Int> {
    val statusBarHeight = getStatusBarHeight()
    val navBarHeight = getNavBarHeight()
    val usableHeight = screenHeight - statusBarHeight - navBarHeight

    val x = (xPercent * screenWidth).toInt() - CHARACTER_WIDTH / 2
    val y = statusBarHeight + (yPercent * usableHeight).toInt() - CHARACTER_HEIGHT / 2

    return Pair(
        x.coerceIn(0, screenWidth - CHARACTER_WIDTH),
        y.coerceIn(statusBarHeight, screenHeight - navBarHeight - CHARACTER_HEIGHT)
    )
}
```

---

## Battery & Background Persistence (ColorOS Fix)

```kotlin
// BatteryOptimizationHelper.kt — called during onboarding
fun requestBatteryExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
```

**Additional ColorOS steps (shown in onboarding UI):**
1. Battery Exemption (handled programmatically above)
2. Auto Launch enabled in ColorOS settings (manual step, guided in onboarding)
3. Persistent notification shown at all times (low priority, keeps service alive)
4. `startForeground()` called in `onCreate()` before any other work

---

## Onboarding Flow (First Launch)

`OnboardingActivity` — 8 steps:

1. **Welcome** — introduce DosCom
2. **Overlay permission** — `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
3. **Accessibility Service** — guide user to enable in Android settings + explain why
4. **Screen capture** — `MediaProjection` system dialog (for Vision fallback only)
5. **Battery exemption** — `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
6. **Auto Launch** — guide to ColorOS settings manually
7. **Audio permission** — for voice input
8. **Vision API key** — optional, only needed for fallback. Skip option available.
9. **Done** — DosCom appears for the first time

---

## GitHub Actions Build

```yaml
# .github/workflows/build.yml
name: Build DosCom APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: DosCom-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## Known Limitations & Mitigations

| Limitation | Cause | Mitigation |
|---|---|---|
| Accessibility can't find icon-only buttons | No text/description in UI tree | Gemini Vision fallback activates automatically |
| Vision API coordinates ±50px accuracy | AI estimation, not exact detection | Confirm ring shown before DosCom walks |
| MediaProjection dialog every service restart | Android security requirement | Shown once per session, result cached |
| ColorOS kills background services | ColorOS battery management | Foreground service + battery exemption + onboarding guidance |
| Accessibility scan can cause lag | UI tree traversal is expensive | Triggered only on user command, never passive |
| Status bar icon positions unknown | Android API limitation | Notification reactions animate in place, low battery points to hardcoded region |
| Voice recognition needs internet | Google SpeechRecognizer is cloud-based | Acceptable — Vision fallback also needs internet |
| Gemini Intent may not pre-fill query | Deep link support varies by Gemini version | Fallback opens Gemini app directly |

---

## What DosCom Is NOT

- ❌ Not always listening (interaction only on long-press)
- ❌ Not a root-level agent
- ❌ Not cross-device perfect (optimized for Oppo ColorOS, works on Android 10+ generally)
- ❌ Not a Gemini replacement — it *uses* Gemini as the AI brain

---

## Summary: Why This Architecture Wins

| Approach | Accuracy | Cost | Works on |
|---|---|---|---|
| Accessibility only | Exact | Free | Text-labeled UI elements |
| Vision API only | ~±50px | API calls | Any visible UI |
| **DosCom (both)** | **Exact when possible, visual fallback** | **Free mostly** | **Everything** |

---

## Project Name
**DosCom** = **Dos**ti + **Com**panion. Your digital dost that lives on your screen.

---

*Built by DeVen | Repo: DeVenLucaz/DosCom | Stack: Kotlin + Jetpack Compose + AccessibilityService + Gemini*
