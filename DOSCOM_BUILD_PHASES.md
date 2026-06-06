# DosCom — Antigravity Build Phases
> Feed each prompt to Antigravity ONE AT A TIME.
> Wait for build to succeed before moving to next.
> Each phase has ONE job. Never combine phases.

---

## PRE-FLIGHT (Run this first, manually in Termux)

```bash
mkdir -p ~/projects/DosCom
cd ~/projects/DosCom
git init
gh repo create DeVenLucaz/DosCom --public --source=. --remote=origin
```

---

---

# ═══════════════════════════════════
# PHASE 1 — PROJECT SKELETON
# ═══════════════════════════════════

## 1.1 — Create Gradle Project Structure

**Goal:** Empty but buildable Android project.

```
Read DOSCOM_PROJECT.md.

Create a new Android Kotlin project with:
- Package name: com.devenlucaz.doscom
- Min SDK: 26 (Android 8)
- Target SDK: 33 (Android 13)
- Build tool: Gradle with Kotlin DSL (build.gradle.kts)
- Empty MainActivity.kt that just calls setContentView
- No Jetpack Compose yet, plain XML layout for now
- Standard folder structure: app/src/main/java/... and app/src/main/res/...
- settings.gradle.kts and root build.gradle.kts

Do not add any features yet. Just a project that compiles.
```

---

## 1.2 — GitHub Actions CI

**Goal:** Every push builds an APK automatically.

```
In the DosCom Android project, create .github/workflows/build.yml

The workflow must:
- Trigger on every push to main branch
- Use ubuntu-latest runner
- Set up Java 17 with temurin distribution
- Run ./gradlew assembleDebug
- Upload the output APK from app/build/outputs/apk/debug/app-debug.apk
  as artifact named "DosCom-debug"
- Cache Gradle dependencies for faster builds

Do not modify any other files.
```

---

## 1.3 — AndroidManifest Permissions

**Goal:** Declare all permissions the app will need upfront.

```
In app/src/main/AndroidManifest.xml, add all required permissions and service declarations for DosCom:

Permissions needed:
- SYSTEM_ALERT_WINDOW
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_MEDIA_PROJECTION
- BIND_ACCESSIBILITY_SERVICE
- RECORD_AUDIO
- INTERNET
- VIBRATE
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- RECEIVE_BOOT_COMPLETED

Also declare these services (empty stubs for now, just declarations):
- CompanionOverlayService (foreground service)
- DosCom AccessibilityService with meta-data pointing to xml/accessibility_service_config

Create res/xml/accessibility_service_config.xml with:
- accessibilityEventTypes: typeWindowContentChanged|typeWindowStateChanged
- accessibilityFeedbackType: feedbackGeneric
- canRetrieveWindowContent: true
- canPerformGestures: true
- notificationTimeout: 100

Do not implement the services yet, only declare them.
```

---

---

# ═══════════════════════════════════
# PHASE 2 — FLOATING OVERLAY
# ═══════════════════════════════════

## 2.1 — Foreground Service Shell

**Goal:** A service that starts, stays alive, shows notification.

```
Create app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt

Requirements:
- Extends Service
- Calls startForeground() immediately in onCreate() before anything else
- Notification channel ID: "doscom_overlay"
- Notification: low priority, title "DosCom is running", no sound
- onStartCommand returns START_STICKY
- Empty onDestroy for now
- No overlay window yet, just the service lifecycle

Also create a ServiceManager.kt utility with:
- fun startOverlayService(context: Context)
- fun stopOverlayService(context: Context)
```

---

## 2.2 — WindowManager Overlay View

**Goal:** A colored square appears floating on screen.

```
In CompanionOverlayService.kt, add WindowManager overlay:

- Get WindowManager via getSystemService
- Create a simple 80x80dp View with a colored background (any color, placeholder)
- WindowManager.LayoutParams with:
  - TYPE_APPLICATION_OVERLAY
  - FLAG_NOT_FOCUSABLE
  - FLAG_NOT_TOUCHABLE (default idle state)
  - WRAP_CONTENT width and height
  - MATCH_PARENT for gravity base
  - Initial position: x=50, y=300
- Add the view to WindowManager in onCreate after startForeground
- Remove the view in onDestroy
- Store layoutParams as a field for later updates
```

---

## 2.3 — Drag Touch Handler

**Goal:** User can drag DosCom around the screen.

```
In CompanionOverlayService.kt, add touch handling to the overlay view:

- When user touches the DosCom view:
  - Remove FLAG_NOT_TOUCHABLE so the view receives touch
  - Track ACTION_DOWN initial touch position and initial layoutParams x/y
  - On ACTION_MOVE: calculate delta and update layoutParams.x and layoutParams.y
    via windowManager.updateViewLayout()
  - On ACTION_UP or ACTION_CANCEL: re-apply FLAG_NOT_TOUCHABLE

Also add screen bounds clamping so DosCom cannot be dragged off screen:
- Get real screen width and height via DisplayMetrics
- Clamp layoutParams.x between 0 and screenWidth - viewWidth
- Clamp layoutParams.y between statusBarHeight and screenHeight - viewHeight - navBarHeight

Create utils/ScreenMetrics.kt with helper functions:
- fun getScreenWidth(context: Context): Int
- fun getScreenHeight(context: Context): Int  
- fun getStatusBarHeight(context: Context): Int
- fun getNavBarHeight(context: Context): Int
```

---

## 2.4 — Edge Snapping

**Goal:** When user releases DosCom, it snaps to nearest screen edge.

```
In CompanionOverlayService.kt, after ACTION_UP in the touch handler, add edge snapping:

- On release, check if layoutParams.x is in left half or right half of screen
- Animate to x=0 (left edge) or x=screenWidth-viewWidth (right edge)
- Use ValueAnimator.ofInt() for smooth snap animation
- Duration: 200ms
- Interpolator: DecelerateInterpolator
- Each animation frame: update layoutParams.x and call windowManager.updateViewLayout()

Do not change y position during snap, only x.
```

---

---

# ═══════════════════════════════════
# PHASE 3 — CHARACTER ANIMATION
# ═══════════════════════════════════

## 3.1 — CharacterState Enum + Renderer Shell

**Goal:** Define all animation states DosCom can be in.

```
Create com/devenlucaz/doscom/character/CharacterState.kt:

enum class CharacterState {
    IDLE_BLINK,
    IDLE_BOB,
    IDLE_LOOK_LEFT,
    IDLE_LOOK_RIGHT,
    WALK_RIGHT,
    WALK_LEFT,
    POINT,
    LISTEN,
    REACT_WAVE,
    REACT_WORRY,
    REACT_HAPPY,
    TALK
}

Create com/devenlucaz/doscom/character/CompanionRenderer.kt:
- Extends View
- Has a currentState: CharacterState field (default IDLE_BOB)
- Has a currentFrame: Int field (default 0)
- onDraw(): draws a simple robot face using Canvas:
  - Body: rounded rectangle (gray)
  - Eyes: two circles (white with black pupil)
  - Antenna: line + small circle on top
  - Legs: two small rectangles
  - All sizes relative to view width/height so it scales
- fun setState(state: CharacterState): updates currentState, resets frame
- fun nextFrame(): increments currentFrame, wraps at max frames per state

No spritesheet yet. Pure Canvas drawing only.
```

---

## 3.2 — Idle Animation Loop

**Goal:** Character blinks and bobs continuously.

```
In CompanionRenderer.kt, add animation loop:

- Use a Handler + Runnable posted with postDelayed for frame timing
- Frame interval: 150ms
- In each frame tick:
  - Call nextFrame()
  - Invalidate the view (triggers redraw)
  
In onDraw(), vary the drawing based on currentState and currentFrame:

IDLE_BOB: offset the entire character y position by sin(frame * 0.5) * 4px (gentle float)

IDLE_BLINK:
  - Frames 0-2: eyes fully open
  - Frame 3: eyes half closed (draw smaller eye circles)
  - Frame 4: eyes closed (draw thin horizontal lines)
  - Frame 5: eyes open again

Add to CompanionOverlayService:
- Replace placeholder colored view with CompanionRenderer
- Start animation loop when service starts
```

---

## 3.3 — Random Idle Behavior Scheduler

**Goal:** Character randomly looks around and blinks on its own.

```
In CompanionOverlayService.kt, add idle behavior scheduler:

- Create a Handler that fires every random interval between 3000ms and 8000ms
- Each time it fires, randomly pick one of:
  - CharacterState.IDLE_BLINK (weight: 40%)
  - CharacterState.IDLE_LOOK_LEFT (weight: 20%)
  - CharacterState.IDLE_LOOK_RIGHT (weight: 20%)
  - CharacterState.IDLE_BOB (weight: 20%)
- Call companionRenderer.setState(pickedState)
- After 1200ms, return to IDLE_BOB

In CompanionRenderer onDraw(), add IDLE_LOOK_LEFT and IDLE_LOOK_RIGHT:
- Shift the eye pupils left or right by 3px relative to normal position
```

---

---

# ═══════════════════════════════════
# PHASE 4 — SCREEN READING
# ═══════════════════════════════════

## 4.1 — AccessibilityService Shell

**Goal:** Service connects and logs that it's active.

```
Create com/devenlucaz/doscom/service/DosCom AccessibilityService.kt:

- Extends AccessibilityService
- onAccessibilityEvent(): do nothing for now (no passive scanning)
- onInterrupt(): do nothing
- onServiceConnected(): Log "DosCom AccessibilityService connected"
- Add a companion object with:
  - var instance: DosCom AccessibilityService? = null
  - fun isConnected(): Boolean = instance != null
- Set instance = this in onServiceConnected
- Set instance = null in a custom disconnect method

This service does NOTHING passively. It only acts when called.
```

---

## 4.2 — UI Tree Scanner

**Goal:** Given a search term, find the matching element on screen and return its coordinates.

```
Create com/devenlucaz/doscom/screen/AccessibilityScanner.kt:

fun findNodeByText(searchTerms: List<String>): AccessibilityNodeInfo? {
  - Get root node from DosCom AccessibilityService.instance?.rootInActiveWindow
  - Traverse the entire UI tree recursively
  - For each node, check:
    - node.text contains any of searchTerms (case insensitive)
    - node.contentDescription contains any of searchTerms (case insensitive)
  - Return the first matching node that isVisibleToUser == true
  - Return null if nothing found
}

fun getNodeCenterCoords(node: AccessibilityNodeInfo): Pair<Int, Int> {
  - Call node.getBoundsInScreen(rect)
  - Return Pair(rect.centerX(), rect.centerY())
}

Important: always call node.recycle() after use to prevent memory leaks.
Add null safety everywhere — rootInActiveWindow can be null if service not connected.
```

---

## 4.3 — Keyword Extractor

**Goal:** Turn a natural language question into search terms for the UI scanner.

```
Create com/devenlucaz/doscom/screen/KeywordExtractor.kt:

fun extractKeywords(query: String): List<String> {
  This is a simple rule-based extractor (no AI needed here):
  
  1. Lowercase the query
  2. Remove filler words: "how", "do", "i", "can", "where", "is", "the",
     "a", "an", "to", "open", "find", "show", "me", "please", "tap",
     "click", "press", "button", "icon", "app"
  3. Split remaining words into a list
  4. Also add the full original query as one entry (for exact matches)
  5. Return the list, most specific terms first
  
Examples:
  "how do I open YouTube" → ["youtube", "how do I open YouTube"]
  "where is the save button" → ["save", "where is the save button"]
  "open CapCut projects" → ["projects", "capcut", "open CapCut projects"]
}
```

---

## 4.4 — CoordinateMapper

**Goal:** Convert raw coordinates (from accessibility or from Vision API percentages) into safe screen positions for DosCom to walk to.

```
Create com/devenlucaz/doscom/screen/CoordinateMapper.kt:

// From AccessibilityNodeInfo — already exact screen coords
fun fromNodeCoords(rawX: Int, rawY: Int, characterSizePx: Int): Pair<Int, Int> {
  - Subtract half characterSizePx from both x and y (so DosCom centers on target)
  - Clamp x: 0 to screenWidth - characterSizePx
  - Clamp y: statusBarHeight to screenHeight - navBarHeight - characterSizePx
  - Return clamped Pair
}

// From Gemini Vision percentages — convert to pixels
fun fromPercent(xPercent: Float, yPercent: Float, characterSizePx: Int): Pair<Int, Int> {
  - usableHeight = screenHeight - statusBarHeight - navBarHeight
  - rawX = (xPercent * screenWidth).toInt()
  - rawY = statusBarHeight + (yPercent * usableHeight).toInt()
  - Then call fromNodeCoords(rawX, rawY, characterSizePx)
}

Use ScreenMetrics helpers for all screen dimension values.
```

---

---

# ═══════════════════════════════════
# PHASE 5 — WALK TO TARGET
# ═══════════════════════════════════

## 5.1 — Walk Animation Engine

**Goal:** DosCom smoothly walks from its current position to any (x, y) on screen.

```
Create com/devenlucaz/doscom/character/CompanionAnimator.kt:

class CompanionAnimator(
    private val windowManager: WindowManager,
    private val characterView: CompanionRenderer,
    private val layoutParams: WindowManager.LayoutParams,
    private val screenWidth: Int
) {
    fun walkTo(targetX: Int, targetY: Int, onArrival: () -> Unit) {
        val startX = layoutParams.x
        val startY = layoutParams.y
        val distance = hypot((targetX - startX).toFloat(), (targetY - startY).toFloat())
        
        // Duration scales with distance: 400ms minimum, 2000ms maximum
        val duration = (distance / screenWidth * 1800).toLong().coerceIn(400, 2000)
        
        // Set correct walk direction sprite
        characterView.setState(
            if (targetX >= startX) CharacterState.WALK_RIGHT else CharacterState.WALK_LEFT
        )
        
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedFraction
                layoutParams.x = (startX + (targetX - startX) * t).toInt()
                layoutParams.y = (startY + (targetY - startY) * t).toInt()
                windowManager.updateViewLayout(characterView, layoutParams)
            }
            doOnEnd {
                characterView.setState(CharacterState.POINT)
                onArrival()
            }
        }.start()
    }
    
    fun walkToEdge(screenWidth: Int, onComplete: () -> Unit) {
        // Walk back to nearest edge after task complete
        val targetX = if (layoutParams.x < screenWidth / 2) 0 else screenWidth - 80.dpToPx()
        walkTo(targetX, layoutParams.y, onComplete)
    }
}
```

---

## 5.2 — Confirm Ring Overlay

**Goal:** Before DosCom walks, a pulsing ring appears at the target so user can verify the position.

```
Create com/devenlucaz/doscom/ui/ConfirmRing.kt:

- A View added to WindowManager at target coordinates
- Draws a circle using Canvas: stroke only, no fill, semi-transparent blue color
- Animates: scale from 0.5 to 1.5 over 500ms, fade out simultaneously
- Repeat once (two pulses total)
- Auto-removes itself from WindowManager after animation completes
- Size: 60x60dp centered on target coordinates

Add to CompanionOverlayService:
fun showConfirmRing(targetX: Int, targetY: Int, onComplete: () -> Unit) {
  - Add ConfirmRing view to WindowManager at targetX, targetY
  - After 1000ms (two pulses): remove view, call onComplete
}
```

---

## 5.3 — Speech Bubble

**Goal:** DosCom shows a short text explanation near its position.

```
Create com/devenlucaz/doscom/ui/SpeechBubble.kt:

- A View added to WindowManager near DosCom's current position
- Layout: rounded rectangle background (white, 8dp corners, subtle shadow)
- Contains: TextView with explanation text, max 2 lines, 13sp
- Positioned above DosCom if DosCom is in bottom half of screen,
  below DosCom if in top half
- Appears with fade-in animation (200ms)
- Auto-dismisses after 4000ms with fade-out (300ms)
- Then removes itself from WindowManager

Add to CompanionOverlayService:
fun showSpeechBubble(text: String) {
  - Remove any existing bubble first
  - Create and show new SpeechBubble
}
```

---

## 5.4 — ScreenReader Orchestrator

**Goal:** Combine accessibility scan + Vision fallback into one unified call.

```
Create com/devenlucaz/doscom/screen/ScreenReader.kt:

data class TargetResult(
    val x: Int,
    val y: Int,
    val explanation: String,
    val source: String,  // "accessibility" or "vision"
    val node: AccessibilityNodeInfo? = null
)

suspend fun findTarget(query: String, screenshot: Bitmap?): TargetResult? {

    // LAYER 1: Accessibility tree
    val keywords = KeywordExtractor.extractKeywords(query)
    val node = AccessibilityScanner.findNodeByText(keywords)
    
    if (node != null) {
        val (rawX, rawY) = AccessibilityScanner.getNodeCenterCoords(node)
        val (x, y) = CoordinateMapper.fromNodeCoords(rawX, rawY, CHARACTER_SIZE_PX)
        return TargetResult(x, y, "Found it — tap here", "accessibility", node)
    }
    
    // LAYER 2: Gemini Vision fallback
    if (screenshot != null && GeminiVisionClient.isConfigured()) {
        val result = GeminiVisionClient.analyze(screenshot, query)
        if (result != null && result.found) {
            val (x, y) = CoordinateMapper.fromPercent(result.xPercent, result.yPercent, CHARACTER_SIZE_PX)
            return TargetResult(x, y, result.explanation, "vision")
        }
    }
    
    return null  // Nothing found
}
```

---

---

# ═══════════════════════════════════
# PHASE 6 — USER INTERACTION
# ═══════════════════════════════════

## 6.1 — Screenshot Helper

**Goal:** Capture current screen cleanly before showing any overlay.

```
Create com/devenlucaz/doscom/utils/ScreenshotHelper.kt:

- Wraps MediaProjection API
- fun requestPermission(activity: Activity): fires MediaProjectionManager intent
- fun onPermissionResult(resultCode: Int, data: Intent): stores projection token
- fun captureScreen(): Bitmap?
  - Uses ImageReader to grab one frame from VirtualDisplay
  - Resizes to max 800px wide maintaining aspect ratio
  - Compresses to JPEG 70% quality
  - Returns Bitmap or null if projection not available

fun bitmapToBase64(bitmap: Bitmap): String
  - Converts to base64 string for API calls

Important: MediaProjection permission result must be cached after first approval.
Do not request permission again unless service restarts.
```

---

## 6.2 — Long Press + Screenshot Trigger

**Goal:** Long press on DosCom captures screen, then shows input.

```
In CompanionOverlayService.kt touch handler, add long press detection:

- On ACTION_DOWN: start a Handler delayed 500ms (long press threshold)
- On ACTION_MOVE beyond 10px: cancel the long press handler
- On ACTION_UP before 500ms: treat as regular tap (reserved for future)
- On long press fires:
  1. Vibrate 50ms (haptic feedback)
  2. characterView.setState(CharacterState.LISTEN)
  3. Call screenshotHelper.captureScreen() — capture IMMEDIATELY
  4. Store bitmap in a field: lastScreenshot
  5. After 300ms delay: show ChatInputOverlay
  6. Remove FLAG_NOT_TOUCHABLE so input is reachable

Do not show any overlay before step 3 completes.
This ordering is critical — screenshot must be clean.
```

---

## 6.3 — Chat Input Overlay

**Goal:** Simple text input appears after long press so user can type their question.

```
Create com/devenlucaz/doscom/ui/ChatInputOverlay.kt:

- A View added to WindowManager at bottom of screen
- Layout:
  - Rounded rectangle card (white background)
  - EditText: "Ask DosCom..." hint, single line, done action
  - Send button (icon only)
  - Small "×" close button top-right
- Appears with slide-up animation from bottom (250ms)
- On send / keyboard done:
  1. Get text from EditText
  2. Dismiss overlay with slide-down animation
  3. Call onQuerySubmitted(query: String, screenshot: Bitmap?)
- On close button: dismiss with slide-down, no callback

Add to CompanionOverlayService:
fun showChatInput(screenshot: Bitmap?) {
  - Create ChatInputOverlay
  - Pass onQuerySubmitted callback that calls handleQuery(query, screenshot)
}
```

---

## 6.4 — Query Handler (Full Pipeline)

**Goal:** Wire everything together — from query to DosCom walking to target.

```
In CompanionOverlayService.kt, add:

fun handleQuery(query: String, screenshot: Bitmap?) {
    // 1. Show DosCom in thinking state
    characterView.setState(CharacterState.IDLE_BOB)
    showSpeechBubble("Let me look...")
    
    // 2. Launch coroutine
    serviceScope.launch {
        val result = screenReader.findTarget(query, screenshot)
        
        withContext(Dispatchers.Main) {
            if (result == null) {
                // Nothing found
                showSpeechBubble("Hmm, I couldn't find that on screen")
                characterView.setState(CharacterState.REACT_WORRY)
                return@withContext
            }
            
            // 3. Show confirm ring at target
            showConfirmRing(result.x, result.y) {
                // 4. Walk to target
                companionAnimator.walkTo(result.x, result.y) {
                    // 5. Show explanation
                    showSpeechBubble(result.explanation)
                    characterView.setState(CharacterState.POINT)
                    
                    // 6. After 4 seconds, walk back to edge
                    Handler(Looper.getMainLooper()).postDelayed({
                        companionAnimator.walkToEdge(screenWidth) {
                            characterView.setState(CharacterState.IDLE_BOB)
                        }
                    }, 4000)
                }
            }
        }
    }
}

Add a serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) to the service.
Cancel it in onDestroy().
```

---

---

# ═══════════════════════════════════
# PHASE 7 — VOICE INPUT
# ═══════════════════════════════════

## 7.1 — SpeechRecognizer Wrapper

**Goal:** Voice input as alternative to typing.

```
Create com/devenlucaz/doscom/service/VoiceInputService.kt:

class VoiceInputService(private val context: Context) {

    fun startListening(onResult: (String) -> Unit, onError: () -> Unit) {
        - Create SpeechRecognizer using SpeechRecognizer.createSpeechRecognizer(context)
        - Intent: RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        - Language model: LANGUAGE_MODEL_FREE_FORM
        - Extra language: Locale.getDefault()
        - Max results: 1
        - onResults: extract top result string, call onResult(text)
        - onError: call onError()
        - Always call destroy() after result or error
    }
    
    fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)
}
```

---

## 7.2 — Voice Trigger

**Goal:** Long press activates voice instead of (or in addition to) text input.

```
In ChatInputOverlay.kt, add a microphone button next to the send button.

When mic button tapped:
1. Hide keyboard if open
2. Show animated listening indicator (antenna blink on DosCom)
3. Call voiceInputService.startListening(
     onResult = { text ->
       editText.setText(text)
       // Auto-submit after 800ms so user sees what was recognized
       Handler.postDelayed({ submitQuery() }, 800)
     },
     onError = {
       showSpeechBubble("Couldn't hear that, try typing?")
     }
   )

In CompanionOverlayService, when voice listening starts:
- characterView.setState(CharacterState.LISTEN)
```

---

---

# ═══════════════════════════════════
# PHASE 8 — GEMINI INTEGRATION
# ═══════════════════════════════════

## 8.1 — Gemini Intent Bridge

**Goal:** For general questions (not "find this on screen"), open Gemini app with the query.

```
Create com/devenlucaz/doscom/ai/GeminiIntentBridge.kt:

fun sendToGemini(context: Context, query: String) {
    // Try deep link first
    val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://gemini.google.com/app?q=${Uri.encode(query)}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (deepLinkIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(deepLinkIntent)
        return
    }
    // Fallback: launch Gemini app package directly
    val launchIntent = context.packageManager
        .getLaunchIntentForPackage("com.google.android.apps.bard")
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        // Gemini not installed, open Play Store
        val playIntent = Intent(Intent.ACTION_VIEW,
            Uri.parse("market://details?id=com.google.android.apps.bard"))
        context.startActivity(playIntent)
    }
}

fun isGeminiInstalled(context: Context): Boolean =
    context.packageManager.getLaunchIntentForPackage(
        "com.google.android.apps.bard") != null
```

---

## 8.2 — Gemini Vision Client (Fallback Only)

**Goal:** When accessibility finds nothing, analyze screenshot via Gemini Vision API.

```
Create com/devenlucaz/doscom/ai/GeminiVisionClient.kt:

data class VisionResult(
    val found: Boolean,
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val explanation: String = "",
    val elementDescription: String = ""
)

class GeminiVisionClient(private val apiKey: String) {

    suspend fun analyze(screenshot: Bitmap, query: String): VisionResult? {
        val base64Image = bitmapToBase64(screenshot)
        
        val prompt = """
You are a screen navigation assistant for Android.
The user is asking: "$query"

Look at this screenshot. Identify the single most relevant UI element to tap.
Respond ONLY in this exact JSON format, no markdown, no extra text:
{
  "element_description": "brief name of element",
  "x_percent": 0.0,
  "y_percent": 0.0,
  "explanation": "one sentence max 12 words",
  "found": true
}
x_percent: 0.0 = left edge, 1.0 = right edge
y_percent: 0.0 = top edge, 1.0 = bottom edge
If no relevant element is visible, return found: false.
        """.trimIndent()
        
        // POST to Gemini API endpoint with base64 image + prompt
        // Parse JSON response safely, strip markdown fences before parsing
        // Return VisionResult or null on error
    }
    
    fun isConfigured(): Boolean = apiKey.isNotEmpty()
}

API endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey
```

---

---

# ═══════════════════════════════════
# PHASE 9 — NOTIFICATION REACTIONS
# ═══════════════════════════════════

## 9.1 — Notification Listener

**Goal:** DosCom reacts when notifications arrive.

```
Create com/devenlucaz/doscom/service/DosCom NotificationListener.kt:

- Extends NotificationListenerService
- onNotificationPosted(sbn: StatusBarNotification):
  
  Determine reaction type:
  - If sbn.notification.extras contains "android.text" with content → REACT_WAVE (new message)
  - If package contains "battery" or notification category == CATEGORY_STATUS → REACT_WORRY
  - Default for any other notification → REACT_WAVE
  
  Send reaction to CompanionOverlayService via LocalBroadcastManager:
  - Action: "com.devenlucaz.doscom.NOTIFICATION_REACTION"
  - Extra: "reaction_type" = "wave" / "worry" / "happy"

In CompanionOverlayService, register LocalBroadcastReceiver:
- On "wave": setState(REACT_WAVE), return to IDLE_BOB after 2000ms
- On "worry": setState(REACT_WORRY), showSpeechBubble("Low battery!"), return after 3000ms
- On "happy": setState(REACT_HAPPY), return after 2000ms

Add to AndroidManifest:
<service android:name=".service.DosCom NotificationListener"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
  <intent-filter>
    <action android:name="android.service.notification.NotificationListenerService" />
  </intent-filter>
</service>
```

---

---

# ═══════════════════════════════════
# PHASE 10 — ONBOARDING & POLISH
# ═══════════════════════════════════

## 10.1 — Onboarding Activity

**Goal:** First launch walks user through all required permissions cleanly.

```
Create com/devenlucaz/doscom/onboarding/OnboardingActivity.kt:

8-step onboarding flow. Each step is a full-screen card with:
- Illustration (simple vector drawable or emoji-style icon)
- Title
- Description
- Action button

Steps:
1. Welcome — "Meet DosCom" — Next button
2. Overlay — "DosCom needs to float on your screen"
   → Button opens Settings.ACTION_MANAGE_OVERLAY_PERMISSION
   → Check Settings.canDrawOverlays() to confirm before proceeding
3. Accessibility — "DosCom needs to read your screen"
   → Button opens Settings.ACTION_ACCESSIBILITY_SETTINGS
   → Check DosCom AccessibilityService.isConnected() to confirm
4. Screen Capture — "DosCom uses this as backup vision"
   → Button fires MediaProjectionManager.createScreenCaptureIntent()
   → Mark confirmed on result
5. Battery — "Keep DosCom alive on Oppo"
   → Button calls BatteryOptimizationHelper.requestBatteryExemption()
6. Auto Launch — "One more Oppo-specific step"
   → Show instructions card: "Go to Phone Manager > App Auto-Launch > Enable DosCom"
   → Manual step, just a Next button
7. Microphone — "For voice commands"
   → Request RECORD_AUDIO permission normally
8. Vision API Key — "Optional: for harder screens"
   → EditText for Gemini API key
   → Skip button available
   → Save to ~/.doscom/config.json with chmod 600 if provided

After step 8: launch CompanionOverlayService and finish activity.
Save onboarding completion flag to SharedPreferences so it never shows again.
```

---

## 10.2 — Config Manager

**Goal:** Safely store and retrieve API key.

```
Create com/devenlucaz/doscom/utils/ConfigManager.kt:

- Config file path: context.filesDir + "/config.json"
  (uses app internal storage — no external storage permission needed)
- On write: set file permissions to owner-read-only

fun saveApiKey(context: Context, key: String)
fun loadApiKey(context: Context): String?
fun hasApiKey(context: Context): Boolean

Store as:
{
  "gemini_vision_api_key": "AIza..."
}

Never log the API key anywhere.
Never include it in crash reports.
```

---

## 10.3 — MainActivity (Entry Point)

**Goal:** App entry point that routes to onboarding or directly starts service.

```
Update MainActivity.kt:

onCreate():
- Check SharedPreferences for "onboarding_complete" flag
- If false: startActivity(OnboardingActivity), finish()
- If true:
  - Check Settings.canDrawOverlays() — if false, go to onboarding
  - Start CompanionOverlayService via ServiceManager.startOverlayService()
  - Show a minimal UI: "DosCom is running" with a Stop button
  - Stop button calls ServiceManager.stopOverlayService() and finishes activity
  
The app's main window is intentionally minimal — DosCom lives on the overlay, not inside the app.
```

---

## ═══════════════════════════════════
## BUILD ORDER SUMMARY
## ═══════════════════════════════════

```
1.1 → 1.2 → 1.3          (Project compiles + CI works)
         ↓
2.1 → 2.2 → 2.3 → 2.4    (Character floats + drags + snaps)
         ↓
3.1 → 3.2 → 3.3           (Character animates + reacts)
         ↓
4.1 → 4.2 → 4.3 → 4.4    (Screen reading layer ready)
         ↓
5.1 → 5.2 → 5.3 → 5.4    (Walk + confirm ring + speech bubble)
         ↓
6.1 → 6.2 → 6.3 → 6.4    (Full interaction pipeline wired)
         ↓                 ← DEMO-ABLE HERE. Core feature works.
7.1 → 7.2                 (Voice input)
         ↓
8.1 → 8.2                 (Gemini integration)
         ↓
9.1                        (Notification reactions)
         ↓
10.1 → 10.2 → 10.3        (Onboarding + polish)
```

---

## ANTIGRAVITY USAGE RULE

> Feed ONE phase prompt at a time.
> Confirm the APK builds and feature works on device.
> Only then move to next phase.
> Never skip a phase — each one depends on the previous.

---

*DosCom | DeVenLucaz | Built phase by phase*
