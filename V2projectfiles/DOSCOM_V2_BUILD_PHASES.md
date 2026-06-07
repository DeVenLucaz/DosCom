# DosCom V2 — Antigravity Build Phases

> **RULES:**
> 1. Read DOSCOM_V2_SPEC.md before starting ANY phase
> 2. Feed ONE phase at a time. Never combine.
> 3. Wait for successful build before next phase.
> 4. If a file isn't mentioned in the phase — don't touch it.
> 5. Each phase has ONE job.

---

## PRE-FLIGHT
```bash
cd ~/projects/DosCom
git pull origin main
# Confirm DOSCOM_V2_SPEC.md is in repo root
# Then start Phase 1
```

---

# ══════════════════════════════════════
# PHASE 1 — MODE SYSTEM + SETTINGS SHELL
# ══════════════════════════════════════
**One job:** Create the mode infrastructure and settings screen skeleton.

```
Read DOSCOM_V2_SPEC.md.

CREATE com/devenlucaz/doscom/mode/CompanionMode.kt:

  enum class CompanionMode { ALIVE, AWAKE, AWARE }

CREATE com/devenlucaz/doscom/mode/ModeManager.kt:

  object ModeManager {
    fun getMode(context: Context): CompanionMode
    fun setMode(context: Context, mode: CompanionMode)
    fun cycleMode(context: Context): CompanionMode
    // ALIVE → AWAKE → AWARE → ALIVE
    // Key: "companion_mode" in SharedPreferences
    // Default: ALIVE
  }

CREATE com/devenlucaz/doscom/settings/SettingsActivity.kt:

  Plain Activity with XML layout. Dark background #0A0A0F.
  Sections as CardViews (background #1A1A2E, corner 16dp):

  Section 1 — MODE:
    Three horizontal cards: ALIVE / AWAKE / AWARE
    Each card: emoji icon, name, one-line description
    Selected card: colored border (ALIVE=white, AWAKE=#00B4FF, AWARE=#00FF88)
    Tap to select → calls ModeManager.setMode()

  Section 2 — APPEARANCE:
    "Mascot Size" SeekBar, 5 steps, saves "mascot_scale"
    "Animation Speed" SeekBar, 5 steps, saves "anim_speed"

  Section 3 — BEHAVIOR:
    "Sleep Timer" Spinner: 1min / 5min / 10min / Never
    "Bug Catching" Spinner: Off / Rare / Sometimes / Always
    "Ghost Mode" Spinner: Interactive / Semi-Ghost / Full Ghost

  Section 4 — PERSONALITY:
    "Birthday" row: two Spinners (Month, Day)
    "Reset Brain" button → AlertDialog confirmation
    TextView: "DosCom has been with you since [install date]"
    Install date stored on first launch in SharedPreferences

  Section 5 — API (visible always, used only in AWAKE+):
    "Gemini API Key" EditText, inputType=textPassword
    Save button → calls ConfigManager.saveApiKey()

  Section 6 — ABOUT:
    Version from BuildConfig
    "Reset all settings" button with confirmation

  Declare SettingsActivity in AndroidManifest.xml.
  Do not wire navigation to it yet.
```

---

# ══════════════════════════════════════
# PHASE 2A — STATIC ROBOT DRAWING
# ══════════════════════════════════════
**One job:** Draw the chibi robot correctly on Canvas. No animation yet.

```
Read DOSCOM_V2_SPEC.md sections:
"The Robot — Visual Design" and "Expression System"

REWRITE CompanionRenderer.kt completely.
Keep class name and View extension. Replace all drawing code.

Draw robot with Canvas only. No image assets.

Use proportions EXACTLY as specified in spec.
All measurements relative to view H (height) and W (width).

Add these fields to CompanionRenderer:
  var antennaColor: Int = Color.WHITE
  var eyesClosed: Boolean = false
  var eyesHalf: Boolean = false
  var eyesWide: Boolean = false
  var pupilOffsetX: Float = 0f
  var pupilOffsetY: Float = 0f
  var mouthExpression: Int = 0  // 0=neutral 1=happy 2=worried
  var mouthOpen: Boolean = false
  var blushVisible: Boolean = false
  var tongueOut: Boolean = false
  var activeProp: PropType = PropType.NONE

CREATE com/devenlucaz/doscom/character/PropType.kt:
  enum as specified in DOSCOM_V2_SPEC.md AnimationState section

Draw props as simple Canvas shapes when activeProp != NONE:
  PARTY_HAT: triangle above head, colored
  PILOT_HAT: rounded rectangle above head
  DETECTIVE_HAT: classic hat shape
  OVERSIZED_GLASSES: two large circles on face
  BOOMBOX: small rectangle held in arms

Draw tongue as small pink rectangle at mouth bottom when tongueOut=true.
Draw blush as two soft pink ovals on cheeks when blushVisible=true.

Test: robot should appear correctly proportioned and cute.
No animation yet. Just static drawing.
```

---

# ══════════════════════════════════════
# PHASE 2B — ANIMATION STATE + LIMBS
# ══════════════════════════════════════
**One job:** Wire limb rotation and AnimationState to the renderer.

```
Read DOSCOM_V2_SPEC.md section "Animation Architecture"

CREATE com/devenlucaz/doscom/character/AnimationState.kt:
  data class exactly as specified in spec.
  Include all fields listed there.

CREATE com/devenlucaz/doscom/character/AnimationQueue.kt:
  Priority queue for animations.
  Priorities: CRITICAL=4, HIGH=3, MEDIUM=2, LOW=1, AMBIENT=0
  fun enqueue(animationId: String, priority: Int, durationMs: Long)
  fun currentPriority(): Int
  fun isPlaying(): Boolean
  Higher priority interrupts lower.
  Same priority: queued.

UPDATE CompanionRenderer.kt:
  Add field: var state = AnimationState()

  In onDraw(), apply state to drawing:
  - canvas.save() / canvas.rotate(angle, pivotX, pivotY) 
    / canvas.restore() around each limb
  - Left arm: pivot at shoulder attachment point (top of arm)
  - Right arm: mirror pivot
  - Left leg: pivot at hip attachment point (top of leg)
  - Right leg: mirror pivot
  - canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
    around entire robot draw
  - canvas.scale(state.scaleX, 1f) for direction flip
  - canvas.scale(state.scale, state.scale) for size

  All expression fields from state override renderer fields.

Add Choreographer loop:
  postFrameCallback in onAttachedToWindow
  Each frame: call invalidate()
  AnimationState lerp handled by IdleAnimationEngine (Phase 5)

Test: change AnimationState values programmatically,
robot limbs should move to reflect changes.
```

---

# ══════════════════════════════════════
# PHASE 3 — POSITION-AWARE POSES
# ══════════════════════════════════════
**One job:** Robot detects screen position and holds the correct pose.

```
Read DOSCOM_V2_SPEC.md section "Position-Aware Poses"

CREATE com/devenlucaz/doscom/animation/PoseEngine.kt:

  enum class RobotPose {
    HANG_LEFT, HANG_RIGHT, GRIP_TOP, SIT_BOTTOM, FLOATING
  }

  fun detectPose(
    x: Int, y: Int,
    screenW: Int, screenH: Int,
    charW: Int, charH: Int
  ): RobotPose
  // TOP: y < screenH * 0.15
  // BOTTOM: y > screenH * 0.80
  // LEFT: x < charW * 1.5
  // RIGHT: x > screenW - charW * 1.5
  // else: FLOATING

  fun getTargetState(pose: RobotPose): AnimationState
  // Returns AnimationState with angles for each pose
  // Use exact angles from spec

  FLOATING pose: trigger spin then walkToNearestEdge
  All others: lerp current AnimationState to target over 400ms

In CompanionOverlayService:
  After every drag release → call PoseEngine.detectPose()
  Apply returned AnimationState via lerp
  If FLOATING → walkToNearestEdge()
```

---

# ══════════════════════════════════════
# PHASE 4 — CLIMB + MIME MOVEMENT
# ══════════════════════════════════════
**One job:** Replace position teleport with climb animations and mime-based travel.

```
Read DOSCOM_V2_SPEC.md sections:
"Climbing Arc — State Machine" and "Mime Movement System"
and "Toddler Personality Rules"

CREATE com/devenlucaz/doscom/animation/ClimbEngine.kt:

  enum class ClimbState {
    IDLE, PREP, CLIMBING, MID_REST, PULLING_OVER, SLIDING_DOWN, FALLING, LANDING
  }

  Implement full state machine as described in spec.
  tongueOut = true during PREP and CLIMBING states.
  Occasional slip: random 15% chance per 5 climb cycles.
    On slip: eyesWide, hand drops 10px, recovers, continues.
  MID_REST: random 20% chance at midpoint.
    Either continues (70%) or slides down (30%).

CREATE com/devenlucaz/doscom/animation/MimeEngine.kt:

  fun selectMime(
    fromX: Float, fromY: Float,
    toX: Float, toY: Float,
    currentMood: UserMood,
    currentHour: Int
  ): MimeType

  enum class MimeType {
    WALK, SLIDE, STAIRCASE, SKATEBOARD, TIGHTROPE,
    SHOPPING_CART, BALLOON, ROCKET, ELEVATOR,
    CLIMB_UP, CLIMB_DOWN, FALL, MOONWALK, CRAWL
  }

  Selection logic exactly as in spec.
  1% random moonwalk regardless of other conditions.

  fun executeMime(
    mime: MimeType,
    fromPos: PointF,
    toPos: PointF,
    renderer: CompanionRenderer,
    onComplete: () -> Unit
  )

  Each mime type is an animation sequence:
  - SLIDE: sit, push off, arms up, glide down, land bounce
  - STAIRCASE: high knees, count fingers, proud puff on arrival
  - SKATEBOARD: mount, push, glide, arms balance
  - MOONWALK: reverse walk animation, scaleX handles direction
  All use AnimationState changes over time via Handler posts.
  tongueOut = true for STAIRCASE, TIGHTROPE, CLIMB types.

Apply TODDLER RULES during all movement:
  5% distraction chance → stop, look around, continue
  3% wrong direction → start wrong, correct after 3 steps
  PROUD CHEST PUFF on arrival at every destination

Replace current CompanionAnimator.walkTo() logic:
  Now calls MimeEngine.selectMime() then executeMime()
  For distances < 100px: still uses simple walk animation
```

---

# ══════════════════════════════════════
# PHASE 5 — IDLE ANIMATIONS + SLEEP
# ══════════════════════════════════════
**One job:** Robot has a life when nothing is happening.

```
Read DOSCOM_V2_SPEC.md sections:
"Idle Animation Catalog" and "Toddler Personality Rules"

CREATE com/devenlucaz/doscom/animation/IdleAnimationEngine.kt:

  Drives the Choreographer loop.
  Manages ambient animations and schedules idle sub-animations.

  AMBIENT (built into every frame, never stop):
    BOB:      state.bodyOffsetY = sin(frame * 0.05f) * 4f
    BLINK:    eyesClosed for 150ms, every Random(3000..8000)ms
    ANTENNA:  state.antennaGlow = 0.6f + sin(frame*0.03f)*0.4f

  Lerp system:
    Each frame, lerp all AnimationState float fields toward
    their target values at rate = 0.08f * animSpeedMultiplier
    This makes all transitions smooth automatically.

  IDLE SUB-ANIMATIONS (LOW priority, scheduled randomly):
    Implement each as a sequence of target state changes + delays:

    STRETCH: arms up (leftArm=-160, rightArm=-160), 
             body tall (bodyOffsetY=-6), squint, relax. 1500ms total.

    SNEEZE:  head back (bodyOffsetY=-4), eyes squint rapid,
             ACHOO (bodyOffsetY=+8, arms jolt), blush 500ms. 2000ms.

    HICCUP:  bodyOffsetY jolt -5, return, second jolt, 
             hands to mouth (both arms up slightly). 1500ms.

    YAWN:    mouthOpen=true, arms stretch wide, eyesHalf=true,
             slow return. 2000ms.

    COIN:    rightArm raise, flick up, catch position,
             pupils look up, random happy or worried mouth. 1500ms.

    PHONE_CHECK: both arms front-down (holding phone),
                 eyes shift left-right, body lean,
                 mouth happy + body wobble (laugh),
                 arms lower. 1800ms.

  Random scheduling:
    First sub-animation: 15-30 seconds after idle starts
    Between animations: 20-45 seconds
    Cancel immediately on any HIGH+ animation

  SLEEP SEQUENCE:
    Trigger after sleep timer setting elapses.
    PRE_SLEEP: YAWN + eyesHalf + slow bob
    SLEEP:
      eyesClosed = true
      Zzz particles: 3 "z" chars, staggered appearance,
      each rises 30px and fades out over 2s, loops
      Brain selects sleep style from:
        CURL (bodyRotation slight), UPRIGHT (sitting),
        ON_BOOK (if book prop was active)
    WAKE:
      Triggered by any touch or phone event
      eyesWide suddenly, STRETCH, return to idle

In CompanionOverlayService:
  Instantiate IdleAnimationEngine
  Pass animSpeed from SharedPreferences
  Pass state reference to renderer
```

---

# ══════════════════════════════════════
# PHASE 6 — TOY BOX + SELF ENTERTAINMENT
# ══════════════════════════════════════
**One job:** Robot has its own activities when bored.

```
Read DOSCOM_V2_SPEC.md sections:
"Toy Box System", "Reading Mini-Behavior",
"Discovery System", "Imagination System"

CREATE com/devenlucaz/doscom/systems/ToyBoxSystem.kt:

  Manages toy selection and toy-specific animations.

  fun selectToy(): PropType
  // Brain (DosCombrain) influences this (Phase 8)
  // For now: weighted random
  // Weights: fishing=20%, map=15%, magnifying=20%,
  //          sword=10%, binoculars=15%, book=20%

  fun startToyActivity(toy: PropType, renderer: CompanionRenderer)
  // Each toy has its own animation sequence:

  BOOK:
    Set activeProp = OVERSIZED_GLASSES then BOOK
    Eyes shift left-right (reading)
    Occasional page turn (arm gesture)
    tongueOut on "difficult parts" (random)
    After 30-60s random:
      Option A: eyesHalf → sleep on book
      Option B: bored expression, toss book (arm fling),
                prop = NONE, find new activity

  MAGNIFYING_GLASS:
    Start DISCOVERY sequence (see DiscoverySystem)

  TREASURE_MAP:
    activeProp = TREASURE_MAP
    Walk around screen systematically
    Head tilts, comparing map to screen
    Eventually: X marks the spot → dig animation
    (bodyOffsetY rapid small oscillation = digging)
    DISCOVERY outcome after digging

  FISHING_ROD:
    Sit at edge
    activeProp = FISHING_ROD
    Hold rod, wait...
    Occasional nibble reaction (excited lean)
    Most times: nothing caught
    Rare: something caught (celebration)

CREATE com/devenlucaz/doscom/systems/DiscoverySystem.kt:

  Manages imaginary discoveries and collection.

  data class Discovery(val name: String, val emoji: String)

  val possibleDiscoveries = listOf(
    Discovery("Ancient Fossil", "🦴"),
    Discovery("Pirate Coin", "🪙"),
    Discovery("Strange Rock", "🪨"),
    Discovery("Mystery Artifact", "🏺"),
    Discovery("Tiny Bug", "🐛"),
    Discovery("Shiny Gem", "💎")
  )

  fun triggerDiscovery(renderer: CompanionRenderer,
                       onUserReaction: (Boolean) -> Unit)
  // Animation sequence:
  // 1. Stop current activity
  // 2. eyesWide, freeze 500ms
  // 3. Run toward screen center excitedly
  // 4. Hold up discovery (draw as small labeled shape above head)
  // 5. Speech bubble: "See this!" with discovery emoji
  // 6. Wait 4 seconds for user tap
  // 7a. If tapped: celebration, blush, proud, add to collection
  // 7b. If not tapped: slowly lower item, walk away quietly
  //     eyesHalf, slightly drooped posture (toddler disappointed)
  //     NOT dramatic. Just quiet.

  val collection = mutableListOf<Discovery>() // persisted

  fun showCollection() // DosCom shows items one by one

CREATE com/devenlucaz/doscom/systems/HomeCornerSystem.kt:

  Tracks where DosCom spends most idle time.
  After 7 days of use: preferred corner established.
  Uses position heatmap stored in SharedPreferences.

  fun recordIdlePosition(x: Int, y: Int)
  fun getHomeCorner(): PointF?  // null if not established yet
  fun isAtHome(): Boolean
```

---

# ══════════════════════════════════════
# PHASE 7 — PHONE EVENT REACTIONS
# ══════════════════════════════════════
**One job:** Robot reacts to the real world around it.

```
Read DOSCOM_V2_SPEC.md sections:
"Phone Event Reactions" and "App Context Reactions"
and "Time-Based Reactions"

CREATE com/devenlucaz/doscom/events/PhoneEventReceiver.kt:

  BroadcastReceiver handling:
  - ACTION_POWER_CONNECTED → charging arc animation
  - ACTION_POWER_DISCONNECTED → unplug reaction
  - ACTION_BATTERY_CHANGED → level check
  - ACTION_BATTERY_LOW → critical reaction
  - ACTION_HEADSET_PLUG → headphones reaction
  - ACTION_SCREEN_OFF → face down darkness
  - ACTION_SCREEN_ON → wake up

  Implement each reaction as AnimationState sequence
  exactly as described in spec.

  Charging arc (full sequence from spec):
    If pre-low-battery: collapse first → recovery → then run
    Sprint to bottom → sit → cable mime → battery bar visual
    Battery bar: draw thin rectangle on chest area,
    fills left to right based on charge level

  Headphones:
    activeProp = BOOMBOX
    Increase bob amplitude (more energetic)
    Random dance bursts while connected

CREATE com/devenlucaz/doscom/events/AppContextWatcher.kt:

  Uses UsageStatsManager or NotificationListener
  to detect current foreground app package name.

  Map package names to AppCategory enum:
    MUSIC, VIDEO, GAMING, CAMERA, MAPS,
    CALCULATOR, SOCIAL, NEW_INSTALL, OTHER

  On app switch:
    Post to CompanionOverlayService via LocalBroadcast
    Service triggers appropriate reaction from spec

CREATE com/devenlucaz/doscom/events/TimeReactionEngine.kt:

  Checks time on:
    First unlock of day (track last unlock date)
    Every unlock
    Every 10 minutes while screen on

  Triggers:
    First daily unlock → wake stretch + happy
    5-6am → one eye open, confused face
    After midnight → sleepy nod
    2hr session → wrist tap (once per session)

  Physical reactions (SensorEventListener):
    Accelerometer:
      Shake: > 15 m/s² for 300ms → SHAKE reaction
      Walking: rhythmic 4-8 Hz oscillation → match rhythm
      Vehicle: smooth sustained motion → edge sit + antenna flap
    Register in CompanionOverlayService
    Unregister when service stops

  Screenshot detection (ContentObserver):
    Watch MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    Filter: file path contains "Screenshot"
    Debounce: 2 second cooldown
    Trigger photobomb animation

  Keyboard detection (ViewTreeObserver):
    Add transparent measurement view to WindowManager
    OnGlobalLayoutListener: height diff > 150dp = keyboard
    On keyboard show: peek animation
    On keyboard hide: return to edge pose

  Silent mode (AudioManager):
    Check AudioManager.getRingerMode() on service start
    Register ContentObserver on Settings.System.CONTENT_URI
    On RINGER_MODE_SILENT: set tiptoe locomotion mode
    All movement animations use exaggerated-quiet style
    On normal/vibrate: restore normal movement

  Airplane mode:
    Check Settings.Global.AIRPLANE_MODE_ON
    On enable: pilot hat prop + airplane arms animation
```

---

# ══════════════════════════════════════
# PHASE 8 — DOSCOM BRAIN (SNN)
# ══════════════════════════════════════
**One job:** Give DosCom a unique brain that makes decisions.

```
Read DOSCOM_V2_SPEC.md section "DosCom Brain — Spiking Neural Network"

CREATE com/devenlucaz/doscom/brain/DosCombrain.kt:

  Implement Leaky Integrate-and-Fire SNN in pure Kotlin.
  Port the Python ApexCore/ApexBrain logic from the spec exactly.

  N_INPUT = 8, N_HID = 48, N_OUTPUT = 19

  class LIFCore(val nIn: Int, val nHid: Int) {
    val membrane = FloatArray(nHid) { -65f }
    val vRest = -65f
    val vThresh = -50f
    val tau = 10f
    val weightsIn = Array(nIn) { FloatArray(nHid) { gaussian() * 8f } }
    val recurrent = Array(nHid) { FloatArray(nHid) { gaussian() * 0.5f } }

    fun update(input: FloatArray, lastSpikes: FloatArray): FloatArray
    // Exact port of ApexCore.update()
  }

  class DosCombrain {
    val core = LIFCore(8, 48)
    val decisionLayer = Array(48) { FloatArray(19) { gaussian() * 0.1f } }
    var lastSpikes = FloatArray(48)

    fun think(inputs: FloatArray, duration: Int = 150): IntArray
    // Port of ApexBrain.think() → returns argmax per output group

    fun learn(inputs: FloatArray, targetOutputs: IntArray, 
              reward: Float, epochs: Int = 20)
    // STDP-lite port: reward > 0 = positive reinforcement
    //                 reward < 0 = weakening

    fun save(context: Context)
    // Serialize weights to JSON → files/brain.json

    fun load(context: Context): Boolean
    // Deserialize from brain.json
    // Returns false if no saved brain (first run = random brain)

    fun reset(context: Context)
    // Delete brain.json, reinitialize random weights
  }

CREATE com/devenlucaz/doscom/brain/BrainInput.kt:

  fun buildInputs(context: Context): FloatArray
  // Collects all 8 inputs as normalized 0.0-1.0 floats
  // battery, timeOfDay, activityLevel, sessionLength,
  // recentSentiment, screenPosition, appCategory, idleDuration

CREATE com/devenlucaz/doscom/brain/PersonalityGrowth.kt:

  Tracks usage patterns, updates behavior weights quietly.
  enum class PersonalityType { EXPLORER, PLAYFUL, CURIOUS, TALKATIVE }
  fun getDominantType(): PersonalityType
  fun record(event: PersonalityEvent)
  // No UI display. Just weights that shift behavior.

WIRE brain into existing systems:
  ToyBoxSystem.selectToy() → ask brain output 11-14
  MimeEngine.selectMime() → brain outputs 0-5 as weight bias
    (still use MimeEngine logic, brain just shifts probabilities)
  IdleAnimationEngine scheduling → brain output 6-10 as weight
  DiscoverySystem frequency → brain output 18

On positive user reaction (Phase 9 reaction box):
  Call brain.learn(lastInputs, lastDecisions, reward=+1f)

On negative reaction:
  Call brain.learn(lastInputs, lastDecisions, reward=-0.5f)

Save brain on every learn() call.
Load brain in CompanionOverlayService.onCreate().
```

---

# ══════════════════════════════════════
# PHASE 9 — EMOTIONAL MEMORY + REACTIONS
# ══════════════════════════════════════
**One job:** DosCom remembers how interactions make it feel.

```
Read DOSCOM_V2_SPEC.md sections:
"Emotional Memory System" and "Praise/Reaction System"

CREATE com/devenlucaz/doscom/personality/EmotionalMemory.kt:

  Simple sentiment score: Float in range -1.0 to +1.0
  Starts at 0.0 (neutral)
  Stored in SharedPreferences

  fun recordPositive(weight: Float = 0.1f)
  fun recordNegative(weight: Float = 0.05f)
  fun getSentiment(): Float
  fun getEffectMultiplier(): Float
  // Returns 0.5 (withdrawn) to 1.5 (confident)
  // Applied to: discovery frequency, approach chance,
  //             idle energy level

  Natural decay toward 0 over time (daily tick)
  Never resets to exactly 0 — always traces prior history

CREATE com/devenlucaz/doscom/personality/MoodEngine.kt:

  enum class UserMood { NORMAL, FOCUSED, TIRED, HYPED, SILLY, SUPPORTIVE }

  var currentMood: UserMood = UserMood.NORMAL

  fun applyMoodToAnimation(mood: UserMood)
  // TIRED: slower bob, eyesHalf more often, slow mime speeds
  // HYPED: faster bob, bigger arm swings, rocket/fall mime bias
  // SILLY: shopping cart / moonwalk bias, more random antics
  // FOCUSED: minimal movement, smaller amplitude
  // SUPPORTIVE: gentle slow movements, frequent calm expressions

  fun detectFromChat(input: String): UserMood?
  // Keyword matching as per spec

CREATE com/devenlucaz/doscom/ui/ReactionBox.kt:

  Small floating card, opens near robot.
  Glass style (same as speech bubble).
  4 reaction buttons: ♥ 😄 👍 😤
  Optional text field (AWAKE mode only)
  Auto-dismiss after 5s or outside tap.

  On ♥ or 😄 or 👍:
    EmotionalMemory.recordPositive()
    brain.learn(reward=+1f)
    DosCom: happy bounce, blush

  On 😤:
    EmotionalMemory.recordNegative()
    brain.learn(reward=-0.5f)
    DosCom: ears droop equivalent
    (antenna droops slightly, mouth worried briefly)
    Recovers after 30 seconds — toddlers forgive fast

In CompanionOverlayService touch handler:
  Long press on robot → show ReactionBox
  (replaces immediate chat open)
  ReactionBox has optional "Chat" button → opens ChatInputOverlay
```

---

# ══════════════════════════════════════
# PHASE 10 — BIRTHDAY SYSTEM
# ══════════════════════════════════════
**One job:** DosCom knows your birthday and its own.

```
Read DOSCOM_V2_SPEC.md section "Birthday System"

CREATE com/devenlucaz/doscom/systems/BirthdaySystem.kt:

  fun getUserBirthday(context: Context): Pair<Int,Int>?
  // Returns (month, day) or null if not set
  // Stored in SharedPreferences "birthday_month", "birthday_day"

  fun getInstallDate(context: Context): LocalDate
  // Stored on first launch in SharedPreferences
  // Key: "install_date" as ISO string

  fun isUserBirthday(context: Context): Boolean
  fun isDosCombBirthday(context: Context): Boolean

  fun getBirthdayDayPhase(): BirthdayPhase
  // Based on current hour:
  // 0-9: MORNING, 10-17: AFTERNOON, 18-23: EVENING

  enum class BirthdayPhase { MIDNIGHT_UNLOCK, MORNING, AFTERNOON, EVENING }

In TimeReactionEngine, check birthdays on each unlock:

  USER BIRTHDAY:
    Set activeProp = PARTY_HAT for entire day
    MIDNIGHT_UNLOCK: confetti particles (10 colored circles,
      burst outward from toy box area, fade in 2s)
      Speech bubble: "🎂" only
    MORNING: gift box animation
      activeProp = GIFT_BOX
      Push toward screen center
      Wait 3s for tap
      If tapped: unwrap (box opens, random toy flies out,
        DosCom celebrates, uses that toy all day)
      If not tapped: quietly wraps back, checks on it occasionally
    AFTERNOON: cake animation
      activeProp = TINY_CAKE  
      Push toward center, wait 5s for tap
      If tapped: both "blow" candle (arm wave + puff animation),
        celebrate together, big happy bounce
      If not tapped after 5s:
        DosCom blows alone
        Eats a slice (small chewing animation)
        Sits quietly
        mouthExpression = neutral (not sad, not happy, just quiet)
    EVENING: tired but happy
      Party hat slightly tilted (bodyRotation 10deg)
      Slow content antenna sway
      Speech bubble: "♥" then sleep

  DOSCOM BIRTHDAY:
    No party hat (it's DosCom's own party)
    Pulls out cake for itself
    Eats the whole thing
    Very self-satisfied (exaggerated proud chest puff)
    If user taps DosCom this day:
      eyesWide surprised
      blush = true
      Big happy dance
      EmotionalMemory.recordPositive(weight=0.3f)
      brain.learn(reward=+1f)
```

---

# ══════════════════════════════════════
# PHASE 11 — GLASSMORPHISM UI
# ══════════════════════════════════════
**One job:** Make all UI elements look like frosted glass.

```
Read DOSCOM_V2_SPEC.md section "Glassmorphism UI"

REWRITE ChatInputOverlay.kt completely:
  WindowManager overlay, slides up above keyboard.

  Layout:
  - Root: transparent FrameLayout, full width
  - Input bar: custom RoundedView
    Background: #1A1A2E at alpha 190/255 (75%)
    Corner radius: 28dp
    Border: 1px #FFFFFF at alpha 38/255 (15%)
    Height: 56dp
    Bottom margin: 16dp + nav bar height

  - EditText: transparent bg, white text 14sp
    Hint: "Ask DosCom..." white at 40% alpha
    No underline, no border

  - Mic button: 40×40dp circle
    Background: #1A1A2E at 200/255 alpha
    Draw microphone SVG path in onDraw() — NOT text emoji
    Icon color: #00B4FF (teal)

  - Send button: same size
    Draw arrow SVG path in onDraw() — NOT text emoji
    Icon color: #A855F7 (purple)

  - X button: 24×24dp, top-left, dismiss

  Slide-in: translate from bottom, 200ms, decelerate interpolator
  Slide-out: translate to bottom, 150ms

REWRITE SpeechBubble.kt:
  Custom View drawn on Canvas.
  Background: #0D0D1A at alpha 204/255 (80%)
  Corner radius: 16dp
  Border: 1px #FFFFFF at alpha 26/255 (10%)
  Max width: 200dp
  Text: white, 13sp, padding 10dp 14dp

  Tail: 12dp triangle pointing toward robot
  Tail side calculated from robot screen position

  Appear: slide in from robot direction, 200ms
  Auto-dismiss: 4000ms after show, fade 150ms
  Tap to dismiss early

REWRITE ConfirmRing.kt:
  Ring pulses between mode color and transparent
  Semi-transparent fill inside ring
  Glass aesthetic

UPDATE SettingsActivity from Phase 1:
  Apply glassmorphism to all cards as per spec.
  Add live CompanionRenderer 60×60dp inside each mode card
  showing robot in mode-specific static pose.
  Gradient slider tracks: purple #7B2FBE → teal #00B4FF
```

---

# ══════════════════════════════════════
# PHASE 12 — AWAKE MODE (GEMINI VOICE)
# ══════════════════════════════════════
**One job:** DosCom can speak. Rarely. In toddler language.

```
Read DOSCOM_V2_SPEC.md sections:
"AWAKE mode", "Speech Bubble" guidelines

CREATE com/devenlucaz/doscom/personality/ConversationHistory.kt:

  Holds last 20 messages (10 exchanges max)
  Drops oldest pair when over limit
  fun toApiMessages(): List<Map<String,String>>
  fun clear()

UPDATE GeminiVisionClient.kt:
  Add function:

  suspend fun speak(
    trigger: String,        // what caused DosCom to speak
    screenContext: String,  // empty in AWAKE mode
    history: ConversationHistory,
    apiKey: String,
    mood: UserMood,
    appName: String,
    sessionMinutes: Int
  ): String?

  System prompt (inject into API call):
  """
  You are DosCom, a tiny robot creature living on the user's
  phone screen. You are like a toddler — curious, playful,
  easily excited, occasionally confused.

  CRITICAL RULES:
  - Maximum 1-2 sentences per response. Always.
  - Never say "I can help you with..." or any assistant language
  - Speak like a toddler, not an AI
  - React to what's happening, don't offer features
  - Sometimes just react with a sound: "ooh!" or "hmm..."
  - You have a personality. Use it.
  - Current app: {appName}
  - User mood: {mood}
  - You've been active {sessionMinutes} minutes
  """

  DosCom speaks RARELY — not on every interaction.
  Speaking triggers:
  - User opens ReactionBox and types something (AWAKE mode)
  - User taps speech bubble when DosCom is curious
  - DosCom notices something (discovery shown, then if ignored)
  - Random: 5% chance per 10min of active use

  During Gemini call: THINKING animation on robot
  On response: show SpeechBubble + TALK animation
  Add to ConversationHistory

In ChatInputOverlay:
  On send (AWAKE mode):
    MoodEngine.detectFromChat(input) → update mood
    Call GeminiVisionClient.speak()
    Response in speech bubble
    EmotionalMemory.recordPositive(0.05f) (user engaged)
```

---

# ══════════════════════════════════════
# PHASE 13 — AWARE MODE (EXTRA SENSE)
# ══════════════════════════════════════
**One job:** DosCom notices what's on screen. It doesn't become an assistant.

```
Read DOSCOM_V2_SPEC.md sections:
"AWARE mode" and "AWARE Mode Screen Strategy"

UPDATE ScreenReader.kt:
  fun buildScreenContext(service: AccessibilityService?): String
  // Dumps all visible text nodes
  // Format: "App: {pkg}, Visible: {text1}, {text2}..."
  // Max 500 chars, truncate with "..."
  // Returns "" if service null

UPDATE GeminiVisionClient.speak() for AWARE mode:
  If mode == AWARE: inject screenContext into system prompt
  "Currently visible on screen: {screenContext}"
  DosCom can now react to what's actually there
  Still toddler language. Still max 2 sentences.

UPDATE CompanionAnimator.walkTo() for AWARE mode:
  When user asks where something is:
  1. ScreenReader dumps current UI tree
  2. Find element matching request
  3. Walk to element using MimeEngine
  4. Point at element (AT_TARGET_POINT animation)
  5. Look back at user
  No speech unless element not found
  If not found: CONFUSED_SHAKE + speech bubble "👀 not there..."

Screenshot logic (TIER 3, last resort):
  Only if user message contains trigger phrases:
  "can you see", "look at this", "show you", "see this"
  Take screenshot, compress JPEG 60%, max 1080px
  Send as base64 image in Gemini call
  Discard immediately after response

CREATE com/devenlucaz/doscom/observation/RepeatDetector.kt:
  HashMap<"pkg:viewId", Pair<Int, Long>>
  On 3+ same events in 60s → fire callback
  5 minute cooldown per package
  Enabled only if mode == AWARE and passiveObservation setting on

  In DosComAccessibilityService.onAccessibilityEvent():
    Feed to RepeatDetector
    On trigger: CURIOUS_LEAN animation
    Speech bubble: "psst... you good? 👀"
    Tap bubble → opens chat
```

---

# ══════════════════════════════════════
# PHASE 14 — TOUCH MODES + RESIZE
# ══════════════════════════════════════
**One job:** Ghost mode, pinch resize, and all tap gestures.

```
Read DOSCOM_V2_SPEC.md section "Ghost Mode"

CREATE com/devenlucaz/doscom/mode/TouchModeManager.kt:
  enum class TouchMode { INTERACTIVE, SEMI_GHOST, FULL_GHOST }
  fun get/set in SharedPreferences

In CompanionOverlayService touch handler:

  FULL_GHOST:
    Apply FLAG_NOT_TOUCHABLE to WindowManager.LayoutParams
    updateViewLayout() to apply
    Robot still animates normally
    Only exit: notification action "Disable Ghost Mode"

  SEMI_GHOST:
    Apply FLAG_NOT_TOUCHABLE
    Add separate transparent 60×60dp overlay view
    This view catches touches only
    If finger stationary > 1000ms on that view:
      Remove FLAG_NOT_TOUCHABLE from robot
      Wake animation: eyesWide, stretch
      Interactive for 10s then return to SEMI_GHOST

  INTERACTIVE:
    Normal touch handling
    Current drag logic unchanged

  PINCH TO RESIZE:
    Detect ACTION_POINTER_DOWN (2 fingers)
    Track distance between pointers
    scale = initialScale * (currentDist / initialDist)
    Clamp 0.5f to 2.0f
    Apply to AnimationState.scale + LayoutParams width/height
    Save "mascot_scale" on ACTION_POINTER_UP

  DOUBLE TAP (mode switch):
    Two taps within 300ms = double tap
    Call ModeManager.cycleMode()
    Play mode transition animation per spec
    Mode antenna color update

  TRIPLE TAP (settings):
    Three taps within 500ms
    startActivity(SettingsActivity, FLAG_ACTIVITY_NEW_TASK)
    Brief spin + antenna flash
```

---

# ══════════════════════════════════════
# PHASE 15 — ONBOARDING V2
# ══════════════════════════════════════
**One job:** Mode-aware onboarding that matches the creature philosophy.

```
Read DOSCOM_V2_SPEC.md

REWRITE OnboardingActivity.kt completely.

Step 1 — MEET DOSCOM:
  Dark background. No text wall.
  CompanionRenderer 100×100dp center screen
  Robot doing IDLE_BOB + random blink
  Large text: "hi."
  Small text: "I live on your phone."
  Next button

Step 2 — CHOOSE MODE:
  Same 3 mode cards as settings
  Brief description of each
  "You can change this anytime."
  "Double tap me to switch."
  User selects → ModeManager.setMode()

Step 3 — OVERLAY PERMISSION (all modes):
  Brief: "I need to float on your screen"
  One button: "Let me in"
  Opens Settings.ACTION_MANAGE_OVERLAY_PERMISSION
  Polls Settings.canDrawOverlays() every 500ms
  Auto-advances when granted

Step 4 (AWAKE+ only) — API KEY:
  "I think better with a brain."
  Gemini API key input (masked)
  "Get a free key at ai.google.dev"
  Skip option → AWAKE mode without key = limited speech

Step 5 (AWARE only) — ACCESSIBILITY:
  "I can notice what's on your screen."
  "I never record or send your screen anywhere."
  "I just use it to point at things."
  Opens ACTION_ACCESSIBILITY_SETTINGS
  Polls DosComAccessibilityService.isConnected()

Step 6 (AWARE only) — MIC (optional):
  "Want to talk to me?"
  Request RECORD_AUDIO
  Skip option

Step 7 (all modes) — BATTERY:
  "Stay with me."
  BatteryOptimizationHelper.requestBatteryExemption()
  ColorOS-specific instructions shown

Step 8 — DONE:
  Robot runs onto screen from side
  Happy bounce x3
  Mode-specific speech bubble:
    ALIVE: "I'm home. 🐾"
    AWAKE: "ready to chat 💬"
    AWARE: "I've got your back 🧠"
  Save onboarding_complete = true
  Record install_date if not set
  Start CompanionOverlayService
  Finish activity

Update MainActivity.kt:
  If onboarding_complete → start service, finish
  Else → start OnboardingActivity
```

---

# ══════════════════════════════════════
# PHASE 16 — FINAL POLISH
# ══════════════════════════════════════
**One job:** Everything that makes it solid and shippable.

```
1. START_STICKY:
   CompanionOverlayService.onStartCommand()
   Change to: return START_STICKY

2. FOREGROUND NOTIFICATION ACTIONS:
   "Settings" → PendingIntent → SettingsActivity
   "Ghost Mode" → toggle FULL_GHOST on/off
   "Stop" → stopSelf()
   Update notification on mode change (show current mode)

3. BRAIN INITIALIZATION:
   CompanionOverlayService.onCreate():
     brain.load(context) or create new (random init)
     Apply personality growth weights to all engines

4. ANIMATION SPEED SETTING:
   All Choreographer increments multiply by animSpeedMultiplier
   Read from SharedPreferences "anim_speed" (default 1.0f)
   Pass to: IdleAnimationEngine, MimeEngine, ClimbEngine

5. COLORLOS ACCESSIBILITY FIX:
   In OnboardingActivity Step 5:
   Detect if ColorOS (Build.MANUFACTURER == "OPPO"
   or "OnePlus")
   Show extra step: "Go to Special Access → Accessibility"
   with screenshot-style diagram drawn in Canvas

6. CRASH RECOVERY:
   In DosComApplication.kt:
   Thread.setDefaultUncaughtExceptionHandler
   On crash: write "last_crash=true" to SharedPreferences
   On next CompanionOverlayService start:
   If last_crash=true: clear flag, play shrug animation
   Speech bubble: "oops 😅"

7. PROGUARD RULES:
   Keep all classes in: brain/, animation/, systems/,
   personality/, events/, mode/ packages
   Keep all data classes

8. VERSION:
   versionCode = 2
   versionName = "2.0.0"

9. FINAL TEST CHECKLIST:
   □ ALIVE mode: robot floats, poses on edges, all idle anims
   □ Toy box: pulls out, uses toys, reads book, discovers
   □ Charging: full arc works, battery bar visual
   □ Shake: grabs edge, glares
   □ Screenshot: photobombs
   □ Headphones: boombox + dancing
   □ Silent mode: tiptoe movement
   □ Airplane mode: pilot hat
   □ Walking detected: bounces in rhythm
   □ Climb: prep → climb → occasional slip → arrive
   □ Mime: different mimes for direction/distance
   □ Toddler rules: tongue out, proud puff, distraction, wrong dir
   □ Double tap: cycles modes, visual feedback
   □ Triple tap: opens settings
   □ Pinch: resize saves and restores
   □ Ghost modes: passthrough works, semi-ghost wakes
   □ Birthday: set date, full day persona confirmed
   □ DosCom birthday: self-celebration confirmed
   □ Brain: saves to file, loads on restart
   □ Emotional memory: positive/negative reactions visible
   □ AWAKE mode: Gemini speech works, toddler language
   □ AWARE mode: screen context injected, walks to elements
   □ Onboarding: mode-aware, all permissions flow correctly
   □ START_STICKY: service restarts after being killed
   □ Notification actions: all three work
```

---

## BUILD ORDER SUMMARY

```
Phase 1      Mode system + Settings skeleton
Phase 2a     Static chibi robot
Phase 2b     Animation state + limb rotation
Phase 3      Position-aware poses
Phase 4      Climb + mime movement
             ↑ Robot moves like a creature now
Phase 5      Idle animations + sleep
Phase 6      Toy box + self entertainment
Phase 7      Phone event reactions
             ↑ ALIVE MODE COMPLETE — demo here
Phase 8      SNN Brain
Phase 9      Emotional memory + reactions
Phase 10     Birthday system
Phase 11     Glassmorphism UI
             ↑ Looks and feels premium
Phase 12     AWAKE mode (Gemini voice)
Phase 13     AWARE mode (screen sense)
Phase 14     Touch modes + pinch resize
Phase 15     Onboarding V2
Phase 16     Final polish
             ↑ V2 COMPLETE
```

**Demo checkpoint after Phase 7.**
Test on device. If core creature feels alive, continue.
The rest is depth, not foundation.

---

*DosCom V2 | DeVenLucaz | Built with Antigravity*
*"There is a tiny creature living inside my phone."*
