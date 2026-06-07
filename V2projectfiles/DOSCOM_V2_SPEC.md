# DosCom V2 — Full Specification

> *"DosCom is not an assistant pretending to be alive.*
> *It is a tiny living creature that occasionally helps."*

**Version:** 2.0
**Author:** DeVenLucaz
**Stack:** Kotlin, Canvas, WindowManager, AccessibilityService, Gemini API (background only)
**Target:** Android 13, ColorOS, Dimensity 800U

---

## Mission

DosCom is a combination of three things:

- 👶 **Toddler** — emotionally immature, easily excited, forgives quickly
- 🎭 **Mime** — communicates through body language first, speech last
- 🐕 **Service Dog** — present, watchful, occasionally helpful, never intrusive

Its purpose is **companionship, curiosity, playfulness, and presence.**

Assistance is a convenience feature. The personality comes first.

AI is invisible infrastructure. The user should never think "the AI detected something." They should think "it noticed."

---

## The Three Modes (Consciousness Levels)

User switches by **double tapping** the robot.
Triple tap opens Settings.

### ALIVE 🐾
```
Permissions: OVERLAY only
No API. No accessibility. No mic.

The creature exists.
It animates, reacts to phone events,
entertains itself, and notices the world.
Pure companion. Zero utility pressure.
```

### AWAKE 💬
```
Permissions: OVERLAY + Gemini API key

Everything in ALIVE plus:
The creature can speak.
Rarely. In toddler language.
Not an assistant. Just a creature
that sometimes has something to say.
Gemini drives the voice. Invisible.
```

### AWARE 🧠
```
Permissions: OVERLAY + API key + Accessibility

Everything in AWAKE plus:
The creature has an extra sense.
It notices what app is open.
It reacts to what's on screen.
It occasionally points at things
like a toddler going "look! look!"
Accessibility is a sense, not a feature.
```

### Mode Switch Visual Feedback
```
→ ALIVE:  robot spins, antenna glows WHITE
→ AWAKE:  robot waves, antenna glows BLUE
→AWARE:  robot salutes, antenna glows GREEN
```

---

## The Robot — Visual Design

**Philosophy:** Pure Canvas drawing. No image assets. No spritesheets. Everything drawn in code. Chibi style. Toddler proportions. Silver color scheme from V1.

### Proportions (base size 80×80dp, scales with user preference)
```
Head:     width=0.55W, height=0.38H
          centered, corner radius H*0.15
          slightly wider than tall (chubby)

Body:     width=0.45W, height=0.32H
          centered, 2px below head
          corner radius H*0.08

Legs:     two legs, width=0.12W, height=0.22H
          left: center-0.10W, right: mirror
          corner radius H*0.05

Arms:     width=0.10W, height=0.26H
          attached to body sides
          corner radius H*0.04

Hands:    circle radius=0.07W
          at bottom of each arm
          2 small finger bumps on top

Eyes:     circle radius=0.08W each
          left: head_center_x - 0.12W
          right: mirror
          large, expressive, main emotion carrier

Pupils:   circle radius=0.04W
          can offset to show eye direction

Antenna:  thin line from head top, length H*0.08
          tip: circle radius=0.03W
          tip color changes with mode

Blush:    soft pink ovals on cheeks
          appear when happy/embarrassed
```

### Colors
```
Body/Head:   #C8C8C8
Eye white:   #FFFFFF
Pupil:       #1A1A1A
Antenna tip: WHITE (ALIVE) / #00B4FF (AWAKE) / #00FF88 (AWARE)
Blush:       #FFB3B3
Shadow:      20% black, soft drop shadow
```

### Expression System
```
eyesClosed:      boolean (blink, sleep)
eyesHalf:        boolean (tired, suspicious)
eyesWide:        boolean (scared, excited)
pupilOffset:     PointF (look direction)
mouthNeutral:    flat line
mouthHappy:      upward curve
mouthWorried:    downward curve
mouthOpen:       oval (yawn, surprise, catch snowflake)
blushVisible:    boolean
tongueOut:       boolean (concentration, effort)
```

---

## Animation Architecture

### Core Principle
```
Single Choreographer.postFrameCallback loop
All state in AnimationState data class
CompanionRenderer.onDraw() reads state → draws
No Thread.sleep(). No separate Handlers for animation.
Handler only for scheduling (sleep timer, events).
```

### AnimationState
```kotlin
data class AnimationState(
    // Limbs (degrees, pivot at attachment)
    var leftArmAngle: Float = 0f,
    var rightArmAngle: Float = 0f,
    var leftLegAngle: Float = 0f,
    var rightLegAngle: Float = 0f,
    // Body
    var bodyOffsetY: Float = 0f,
    var bodyOffsetX: Float = 0f,
    var bodyRotation: Float = 0f,
    var scaleX: Float = 1f,  // -1 = facing left
    // Expression
    var eyesClosed: Boolean = false,
    var eyesHalf: Boolean = false,
    var eyesWide: Boolean = false,
    var pupilOffsetX: Float = 0f,
    var pupilOffsetY: Float = 0f,
    var mouthExpression: Int = 0,
    var mouthOpen: Boolean = false,
    var blushVisible: Boolean = false,
    var tongueOut: Boolean = false,
    // Special
    var antennaGlow: Float = 1f,
    var scale: Float = 1f,
    // Props (drawn as simple Canvas shapes)
    var activeProp: PropType = PropType.NONE
)

enum class PropType {
    NONE, PARTY_HAT, SUNGLASSES, PILOT_HAT,
    DETECTIVE_HAT, PIRATE_HAT, OVERSIZED_GLASSES,
    BOOK, FISHING_ROD, TREASURE_MAP, MAGNIFYING_GLASS,
    BOOMBOX, TINY_PHONE, TINY_CAKE, PARTY_HORN,
    CARDBOARD_SWORD, BINOCULARS, UMBRELLA,
    FLASHLIGHT, ABACUS, GIFT_BOX
}
```

### Animation Priority Queue
```
CRITICAL (4): shake, crash, alarm
HIGH     (3): event reactions, walking to target
MEDIUM   (2): phone event reactions, weather
LOW      (1): idle sub-animations, self-entertainment
AMBIENT  (0): bob, blink, antenna pulse (always run)

Higher priority interrupts lower.
Same priority: queued, plays after current.
```

---

## Toddler Personality Rules

These apply to EVERY animation without exception:

```
CONCENTRATION TONGUE:
Any effort task → tongueOut = true
(climbing, tightrope, counting stairs,
reading, fixing things)

PROUD CHEST PUFF:
After arriving at destination
or completing any task
bodyOffsetY brief negative (stands taller)
chin up, mouthHappy
"I did it" energy for even tiny things

DISTRACTION (5% chance mid-journey):
Stops mid-movement
Looks at something imaginary
Forgets where it was going
Shakes head, remembers, continues

WRONG DIRECTION (3% chance):
Starts walking wrong way
3-4 steps
Stops
Looks at destination, looks back
Sheepish expression
Corrects course

SHOWING OFF:
If screen on + no user interaction 30sec+
Do current animation with extra flair
Check if user is watching after
blushVisible if they are

TODDLER ATTENTION SPAN:
Any activity can be abandoned
at random for something more interesting
No activity completes 100% of the time
except event reactions
```

---

## Movement System

### Position-Aware Poses
```
LEFT/RIGHT EDGE:
One hand grips edge
Body hangs, legs sway gently
Grip hand has strain detail

TOP AREA (y < 15% screen):
Both hands grip top
Legs kick freely
Pull-up pose

BOTTOM AREA (y > 80% screen):
Sitting on edge
Legs dangling over
One hand behind head, relaxed

MIDDLE (no edge):
Confused spin
Then walks to nearest edge
Middle is never a resting position
```

### Climbing Arc — State Machine
```
IDLE_AT_POSITION
    ↓ (decides to climb)
PREP_TO_CLIMB:
  Looks up the edge
  Shakes hands loose
  Deep breath (chest expand)
  tongueOut = true (concentrating)
    ↓
CLIMBING (loop):
  Hand grabs higher → pull
  Feet push off
  Other hand grabs → pull
  Horizontal walk animation rotated 90°
  Occasional slip: hand drops 10px
  eyesWide on slip, catches self, continues
    ↓
MID_CLIMB_REST (random, lazy):
  One hand grips
  Other hand free, swings
  Looks around casually
  Either continues or gives up
    ↓
PULLING_OVER_TOP (if going to top):
  Both hands on top edge
  Big effort, body swings up
  Feet kick
  Flops over exhausted
  Wipes forehead
  Settles into GRIP_TOP pose
    ↓
SLIDING_DOWN (if giving up or going down):
  Releases one hand
  Slow slide
  Small friction lines
  eyesHalf (slight panic)
  Lands with bounce
  Sits, relieved
```

### Mime Movement System

**Philosophy:** DosCom doesn't just move between positions. It imagines the screen has physical props and uses them. Every journey is a little story.

**MimeEngine selects based on:**
```
distance:    < 100px → walk
             100-300px → skateboard/tightrope
             300px+ → slide/staircase/rocket
direction:   down → slide preferred
             up → staircase preferred
mood:        HYPED → rocket/fall on purpose
             TIRED → elevator/crawl
             SILLY → shopping cart/balloon/moonwalk
time:        11pm+ → crawl (sleepy)
random 1%:   moonwalk regardless of anything
```

**Going DOWN:**
```
SLIDE (most common):
  Spots destination, eyes light up
  Sits on imaginary slide
  Pushes off with both hands
  Arms up, pure joy
  Antenna streaming back
  Lands, bounces, happy dance

CLIMB DOWN EDGE:
  Careful, focused face
  tongueOut = true
  Hand over hand down
  Slightly uncoordinated (toddler)

ELEVATOR (rare, silly):
  Draws box around itself
  Presses imaginary button
  Stands still while moving
  Ding! Steps out, looks proud

FALL (hyped mood):
  Just jumps off
  Arms flailing
  Lands, dazed stars
  Gets up, acts casual
  "I meant to do that"
```

**Going UP:**
```
STAIRCASE (default):
  High knees, deliberate steps
  Counts on fingers
  Sometimes miscounts, goes back one
  Arrives: proud chest puff
  tongueOut during climb

CLIMB EDGE:
  Hand over hand
  tongueOut = true
  Occasional slip

ROCKET (hyped only, rare):
  Crouches, countdown on fingers
  Launches upward
  Overshoots destination
  Comes back embarrassed

BALLOON (silly mood):
  Grabs imaginary string
  Floats up gently, swaying
  Lets go at destination
  Waves at balloon leaving
```

**Going SIDEWAYS:**
```
SKATEBOARD (default):
  Mimes getting on board
  One foot push-off
  Glides, arms out for balance
  Kicks to speed up if far

TIGHTROPE (focused mood):
  Arms wide for balance
  Tiny careful steps
  Wobbles occasionally
  Mini celebration at destination
  tongueOut = true

SHOPPING CART (silly):
  Both hands on cart
  Running push then hops on
  Coasts, occasionally swerves

MOONWALK (1% random):
  Just moonwalks
  No explanation
  Looks at you after
  Acts normal
```

---

## Self-Entertainment System

When idle, DosCom creates its own activities. It does not wait for the user.

### Toy Box System
```
Source of all self-entertainment.
DosCom pulls toy box onto screen when bored.
Searches through items (rummaging animation).
Selects activity.
Brain (SNN) influences which toy is chosen.

V2 Toys:
- Fishing rod
- Treasure map
- Magnifying glass
- Cardboard sword
- Binoculars
- Detective hat
- Book + oversized glasses
```

### Reading Mini-Behavior
```
Full sequence:
1. Pulls book from off-screen
2. Pulls out oversized glasses (prop)
3. Reads seriously, pages turn
4. Gets distracted (looks around)
5. Gets sleepy (eyesHalf)
6. Either: falls asleep on book
   Or: gets bored, tosses book, 
       finds something else
tongueOut when reading difficult parts
```

### Discovery System
```
DosCom explores screen looking for things.
Discoveries are imaginary.

Examples: fossils, pirate coins, strange rocks,
ancient artifacts, mysterious objects

Behavior:
→ Explore with magnifying glass / binoculars
→ Discover (eyes wide, freeze)
→ Run toward user excitedly
→ Show discovery (holds it up)
→ Speech bubble: "See this!"

If user interacts: celebration, proud pose
If user ignores:
→ Waits, looks at user
→ Slowly lowers item
→ Walks away quietly disappointed
(toddler disappointment, not guilt-trip)

Discoveries go to Home Corner collection.
DosCom occasionally cleans/rearranges them.
```

### Home Corner System
```
DosCom gradually adopts a preferred spot.
Based on where it spends most idle time.
Develops over first week of use.

Corner contains:
- Toy box
- Collection shelf (discovered items)
- Pillow (for sleep)

Users should eventually think:
"That's DosCom's corner."
```

### Imagination System
```
DosCom sees ordinary things and imagines more.

Charging cable → snake (investigates carefully)
Notification → UFO (watches it fly by)
Screen edge → mountain cliff (peers over carefully)
App icon → mysterious portal (suspicious)

These affect only DosCom's animations.
Never the actual UI.
```

---

## Idle Animation Catalog

**Ambient (always, never interrupted):**
```
BOB:       bodyOffsetY = sin(frame * 0.05f) * 4f
BLINK:     eyesClosed for 150ms every 3-8s random
ANTENNA:   antennaGlow = 0.6 + sin(frame * 0.03f) * 0.4
```

**Idle sub-animations (LOW priority):**
```
STRETCH:   arms up wide, stand taller, squint, relax
SNEEZE:    build up, ACHOO jolt, blush after
HICCUP:    body jolt x2, hands cover mouth
YAWN:      mouthOpen wide, arms stretch, eyesHalf
COIN:      flip, catch, react to result
PHONE_CHECK: mime scrolling, laugh, put away
DOODLE:    arm traces shape in air, fades
```

**Sleep (after configurable timer):**
```
PRE_SLEEP: yawn, slow blink, drooping
SLEEP:     eyesClosed, Zzz particles float up
  Styles (brain chooses):
  - Curled up
  - Hanging position (if on edge)
  - Sleeping on book (if was reading)
  - Using toy box as pillow
  - Sitting upright asleep
WAKE:      startled, wide eyes, stretch
```

**Easter eggs:**
```
TAP_DANCE:    2% chance after 10min idle
              Left/right leg taps, jazz hands
BUTTERFLY:    5% chance after 5min idle (once/session)
              Lands on antenna, robot freezes completely
              Flies away, robot waves sadly
SHADOW_PLAY:  10% chance after 10min idle
              Waves at own shadow, shadow waves back delayed
              Pokes shadow, confused
```

---

## Phone Event Reactions

### Charging
```
PLUG IN:
  Check if battery was critically low first
  If yes: dramatic collapse → slow recovery → then proceed
  Sprint to bottom edge
  Sit down, mime plugging cable into belly
  Tiny battery bar appears on chest, fills slowly
  While charging: occasional contented pat of belly
  Random: falls asleep mid-charge

FULLY CHARGED:
  Flexes both arms
  Pulls out tiny flag, plants it proudly
  Victory lap across screen

UNPLUG:
  Stands up
  Waves goodbye to cable
  Returns to normal

BATTERY NOT RISING (bad cable):
  Inspects cable suspiciously
  Wiggles it
  Shrugs
```

### Physical Reactions
```
SHAKE:
  Grabs nearest edge, hangs on
  Body tilts with shake
  On stable: narrows eyes, looks at user judgmentally
  Returns after 1 second

SCREENSHOT:
  Runs to center
  Poses: finger guns OR peace sign (alternates random)
  Freeze 1 second, blush
  Returns to edge

FACE DOWN (dark):
  Pulls out flashlight toy
  Explores the dark
  Falls asleep eventually

WALKING DETECTED (accelerometer):
  Bounces to walking rhythm
  If running: holds on for dear life
  Excited energy

IN VEHICLE (smooth motion):
  Sits at edge
  Antenna flapping in imaginary wind
  Watches "scenery" go by
  Dog-head-out-window energy
```

### Connectivity
```
WIFI CONNECTED:
  Happy spin
  Antenna glows stronger briefly

WIFI LOST:
  Taps antenna confused
  Looks around
  Shrugs, finds toy

NO SIGNAL:
  Holds antenna up high
  Trying to get signal
  Gives up, plays instead

AIRPLANE MODE ON:
  Pulls out pilot hat from toy box
  Walks around importantly
  Makes airplane arms
  Very self-serious about it
```

### Keyboard & Input
```
KEYBOARD OPEN:
  Walks near keyboard top edge
  Peeks over, watching

PASSWORD FIELD (AWAKE+ only):
  Covers eyes with both hands
  eyesWorried briefly
  Stays covering until keyboard closes

FAST TYPING:
  Cheers you on
  Tiny pom poms (drawn as circles)

SNOOZE (alarm snoozed):
  Immediately goes back to sleep
  Snooze solidarity
  No judgment
```

### Silent & Do Not Disturb
```
SILENT MODE ON:
  Tiptoes everywhere
  All movements exaggerated-quiet
  Shushes imaginary things
  Whisper energy for entire duration
```

### Headphones
```
HEADPHONES PLUGGED IN:
  Perks up immediately
  Pulls out boombox toy
  Bobs to imaginary music
  More energetic overall while connected

HEADPHONES REMOVED:
  Boombox goes away
  Returns to normal energy
```

---

## App Context Reactions (ALIVE mode, no accessibility needed)
Uses package name detection only. Not screen reading.

```
MUSIC APP (Spotify, YT Music etc):
  Dancing, boombox toy
  Matches imaginary beat

LONG VIDEO (YouTube, 10min+):
  Sits down comfortably
  Watches too
  Gets bored if very long
  Falls asleep eventually

GAMING APP:
  Excited, cheers at screen
  Covers eyes at tense moments
  Celebrates randomly

CAMERA APP:
  Poses immediately
  Tries different poses
  Fixes antenna
  Very vain energy

MAPS/NAVIGATION:
  Pulls out treasure map
  Pretends to navigate
  Points confidently in wrong direction
  Embarrassed when route changes

CALCULATOR:
  Pulls out tiny abacus
  Tries to calculate too
  Gets confused
  Gives up

NEW APP INSTALLED:
  Investigates curiously
  Sniffs it (toddler examining)
  Random thumbs up or suspicious look

APP DELETED:
  Watches it disappear
  Waves goodbye solemnly
  Moment of silence
  Moves on immediately (toddler attention span)
```

---

## Time-Based Reactions

```
FIRST UNLOCK OF DAY:
  DosCom wakes up, stretches
  Happy to see user
  Like a pet that was waiting

ROUTINE UNLOCK (same time pattern):
  Already awake and ready
  Expecting you

EARLY MORNING (5-6am):
  One eye open
  Confused energy
  "why are we awake" face

LATE NIGHT (after midnight):
  Visibly sleepy
  Keeps nodding off
  Eventually sleeps next to user
  Companionable tired energy

LONG SESSION (2hrs+):
  Taps wrist
  Points away from screen
  Not pushy, just notices
  Once per session only
```

---

## Birthday System

### Setup
```
In app settings:
"When's your birthday?"
[Month spinner] [Day spinner]
No year needed.

"DosCom will keep this between you two."

DosCom's birthday = install date (read-only)
"DosCom has been with you since [date]"
```

### User's Birthday (full day persona change)
```
MIDNIGHT FIRST UNLOCK:
  Already awake
  Confetti from toy box
  Party hat on (stays ALL DAY)
  Speech bubble: 🎂 only

MORNING:
  Presents wrapped gift
  Pushes toward user
  If tapped: unwraps excitedly
  Inside: random toy, plays with all day
  If ignored: quietly wraps it back
  Checks on it occasionally

THROUGHOUT DAY:
  Party horn randomly
  Throws confetti, cleans up after
  Practices dance moves
  Keeps peeking at user excitedly

AFTERNOON:
  Pulls out tiny birthday cake
  One candle
  Pushes toward user, waits...
  If tapped: helps blow candle, celebrates together
  If ignored 30sec: blows alone, eats a slice quietly
  (No drama. Just a toddler eating birthday cake alone.)

APP REACTIONS MODIFIED:
  Camera: poses very seriously (important photo day)
  Maps: assumes going somewhere fun, excited pointing
  Social: looks hopefully at screen

LAST UNLOCK OF DAY:
  Party hat slightly crooked
  Corner messy with confetti
  Sits contentedly
  Antenna slow gentle sway
  Speech bubble: tiny heart
  Sleeps with party hat on
```

### DosCom's Birthday (install anniversary)
```
Different energy: celebrates ITSELF

Pulls out cake for itself
Eats it without sharing
Very self-satisfied

If user taps on this day:
  Surprised and genuinely touched
  Wasn't expecting it
  Big happy reaction
  Emotional memory +1 (positive)
```

---

## Emotional Memory System

DosCom remembers recent interactions emotionally.
All effects are temporary. No guilt-tripping. No manipulation.

### Positive Interactions
```
Triggers: praise in chat, tapping/playing with robot,
          frequent interaction, tapping birthday gift,
          acknowledging DosCom's discoveries

Effects (build up over time):
  More energetic
  More playful
  More confident approaching user
  More frequent self-entertainment
  Discoveries shown more often
```

### Negative Interactions
```
Triggers: repeated ignoring of discoveries,
          ignoring birthday interactions,
          long periods without any interaction

Effects (temporary, fade after positive interaction):
  Quieter behavior
  Hesitates before approaching
  Discoveries kept to itself more
  Stays in home corner more
```

### Praise/Reaction System
```
Dedicated chat box opens:
  When: user long-presses robot → reaction button
  Or: small heart button in AWAKE mode chat

Options:
  ♥ (love)
  😄 (happy)
  👍 (good)
  😤 (annoyed)

Each reaction feeds into emotional memory.
Simple, fast, optional.
No gamification. No scores shown to user.
DosCom just... remembers.
```

---

## DosCom Brain — Spiking Neural Network

### Philosophy
Not AI features. Not an assistant brain.
A personality generator that makes each DosCom unique.
Random weight initialization = unique creature per install.
Weights persist and evolve = creature grows over time.

### Architecture
```kotlin
// 8 inputs, 48 hidden neurons, N outputs
// Leaky Integrate-and-Fire neurons

Inputs (normalized 0.0-1.0):
  0: battery level
  1: time of day
  2: user activity level (idle/typing/active)
  3: session length
  4: recent interaction sentiment (positive/negative)
  5: current position on screen (edge/middle/top/bottom)
  6: app category (entertainment/productivity/creative/other)
  7: idle duration

Outputs (decisions):
  0-5:   which mime to use (6 options)
  6-10:  which idle animation (5 options)
  11-14: which toy to pick (4 options)
  15:    approach user or stay back
  16:    show discovery or keep it
  17:    energy level (affects animation speed)
  18:    discovery frequency bias
```

### Uniqueness
```
weights_in = Random.nextGaussian() * 8f  per install
recurrent  = Random.nextGaussian() * 0.5f per install

Two DosComs on different phones:
Same inputs → different decisions
Same situation → different behavior
Same owner → creature develops differently
```

### Learning (STDP-lite)
```
Positive reaction from user
→ strengthen pathways that led to current behavior
→ DosCom does similar things more in similar situations

Negative reaction / ignored repeatedly
→ weaken those pathways
→ DosCom tries different approaches

Brain learns what its owner enjoys.
Not through programming. Through use.
```

### Hybrid Architecture
```
HARDCODED (consistent, always the same):
  Charging cable → run to bottom
  Shake → grab edge → glare
  Screenshot → photobomb
  Alarm → panic
  Birthday → full persona change
  Low battery → loyalty moment

BRAIN-DRIVEN (unique per install):
  Idle animation selection
  Toy box item choice
  Mime style selection
  Whether to approach user
  Discovery frequency
  Energy level
  Activity duration
```

### Persistence
```
Brain weights saved to file on device:
  /data/data/com.devenlucaz.doscom/files/brain.json

Never reset automatically.
User can reset in Settings (with confirmation).
"This will reset DosCom's personality. Are you sure?"
```

### Personality Growth (emerges from brain + usage)
```
Not levels. Not gamification.
Just behavior weights that shift quietly.

EXPLORER TYPE (frequent discoveries, map toy):
  Digs more, searches more, shows treasures often

PLAYFUL TYPE (frequent toy interactions):
  More games, more antics, more silly behavior

CURIOUS TYPE (user reads/creates a lot):
  Watches more, investigates more, magnifying glass

TALKATIVE TYPE (frequent chat in AWAKE mode):
  More speech bubbles, more reactions

Each user's DosCom slowly becomes unique.
No two are the same after 2 weeks.
```

---

## Glassmorphism UI

### Chat Input Bar (AWAKE+ mode)
```
Background:    #1A1A2E at 75% opacity
Border:        1px #FFFFFF at 15% opacity
Corner radius: 28dp (pill)
Height:        56dp
Margin bottom: 16dp + nav bar

Mic button:    SVG microphone icon, teal
Send button:   SVG arrow icon, purple
Both:          40×40dp circular, glass background
Close (X):     small, dismisses overlay
```

### Speech Bubble
```
Background:    #0D0D1A at 80% opacity
Border:        1px #FFFFFF at 10% opacity
Corner radius: 16dp
Max width:     200dp
Text:          white, 13sp
Tail:          points toward robot
Appear:        slide in from robot direction 200ms
Dismiss:       fade out after 4s or on tap

AWAKE mode speech is RARE and SHORT.
Max 2 sentences. Toddler language.
Never assistant language.
Never "I can help you with..."
```

### Reaction Chat Box
```
Small floating card near robot
Opens on long-press → reaction button
4 reaction icons: ♥ 😄 👍 😤
Optional text input for AWAKE mode
Glass style matching speech bubble
Dismisses after 5s or on outside tap
```

---

## Settings Screen

```
Background: gradient #0A0A0F → #1A1A2E
Cards: glass, #1A1A2E, corner 16dp,
       1px #FFFFFF at 20% opacity border

━━━ MODE ━━━
3 cards horizontal: ALIVE / AWAKE / AWARE
Each: icon, name, one-line description
Each: small live CompanionRenderer 60×60dp
  ALIVE card: robot doing idle bob
  AWAKE card: robot waving
  AWARE card: robot in scan pose
Selected: colored border (white/blue/green)
Unselected: 50% opacity

━━━ APPEARANCE ━━━
Mascot Size:      slider 0.5x → 2.0x
Animation Speed:  slider 0.5x → 2.0x

━━━ BEHAVIOR ━━━
Sleep Timer:      1min / 5min / 10min / Never
Bug Catching:     Off / Rare / Sometimes / Always
Ghost Mode:       Interactive / Semi-Ghost / Full Ghost

━━━ PERSONALITY ━━━
Birthday:         [Month] [Day] input
Reset Brain:      button (with confirmation dialog)
"DosCom has been with you since [install date]"

━━━ API (AWAKE+ only) ━━━
Gemini API Key:   masked input field
Save button

━━━ ABOUT ━━━
Version
Reset all settings (confirmation)
```

### Ghost Mode
```
INTERACTIVE:
  Normal touch. Long press → chat. Drag. Pinch. Double/triple tap.

SEMI-GHOST:
  Touch passes through immediately
  Hold still 1 second on robot → wakes up → Interactive temporarily
  Returns to Semi-Ghost after 10s no interaction

FULL GHOST:
  FLAG_NOT_TOUCHABLE permanent
  Purely visual
  Reacts to phone events still
  Exit only via notification action "Disable Ghost Mode"
```

---

## Technical Architecture

### Package Structure
```
doscom/
├── mode/
│   ├── CompanionMode.kt       (ALIVE/AWAKE/AWARE enum)
│   └── ModeManager.kt         (get/set/cycle)
├── character/
│   ├── CompanionRenderer.kt   (Canvas drawing)
│   ├── AnimationState.kt      (all state)
│   ├── AnimationQueue.kt      (priority queue)
│   └── PropType.kt            (enum)
├── animation/
│   ├── IdleAnimationEngine.kt (ambient + idle scheduling)
│   ├── ClimbEngine.kt         (climb state machine)
│   ├── MimeEngine.kt          (mime selection + execution)
│   └── BugSystem.kt           (bug entities + behavior)
├── brain/
│   ├── DosCombrain.kt         (SNN implementation)
│   ├── BrainInput.kt          (input builder)
│   └── PersonalityGrowth.kt   (type tracking)
├── personality/
│   ├── EmotionalMemory.kt     (sentiment tracking)
│   ├── MoodEngine.kt          (6 mood states)
│   └── ConversationHistory.kt (AWAKE mode, max 20 msgs)
├── systems/
│   ├── ToyBoxSystem.kt        (toy selection + animation)
│   ├── DiscoverySystem.kt     (exploration + collection)
│   ├── HomeCornerSystem.kt    (preferred location tracking)
│   └── BirthdaySystem.kt      (user + DosCom birthdays)
├── events/
│   ├── PhoneEventReceiver.kt  (battery, headphones, etc)
│   ├── AppContextWatcher.kt   (package name reactions)
│   └── TimeReactionEngine.kt  (time-based behaviors)
├── service/
│   ├── CompanionOverlayService.kt
│   ├── DosComAccessibilityService.kt
│   ├── DosComNotificationListener.kt
│   └── VoiceInputService.kt
├── ui/
│   ├── ChatInputOverlay.kt
│   ├── SpeechBubble.kt
│   ├── ReactionBox.kt
│   └── ConfirmRing.kt
├── settings/
│   └── SettingsActivity.kt
├── onboarding/
│   └── OnboardingActivity.kt
└── utils/
    ├── ConfigManager.kt
    ├── ModeManager.kt
    └── BatteryOptimizationHelper.kt
```

### Service Persistence (ColorOS)
```
onStartCommand returns START_STICKY
startForeground() called FIRST in onCreate()
Notification: persistent, low priority
Actions: Settings | Ghost Mode | Stop
BatteryOptimizationHelper in onboarding
```

### AWARE Mode Screen Strategy
```
TIER 1 (always, free):
  AccessibilityService tree dump
  All visible text as plain string
  App name, button labels, content

TIER 2 (on chat send):
  Text dump injected into Gemini prompt
  No image. No screenshot.
  "App: X, Elements: [text1, text2...]"
  Max 500 chars

TIER 3 (explicit request only):
  Screenshot if user says
  "can you see this" / "look at this"
  Compressed JPEG 60%, max 1080px
  Discarded immediately after
```

---

## What DosCom Is NOT

```
❌ "Ask me anything"
❌ "I can help you with..."
❌ Suggestions and recommendations  
❌ Productivity tool
❌ Smart assistant
❌ Always listening
❌ Storing data to server
❌ Guilt-tripping or manipulation
❌ Levels or gamification shown to user
```

## What DosCom IS

```
✅ A tiny creature living on your screen
✅ That has moods and habits
✅ That gets bored and entertains itself
✅ That notices you sometimes
✅ That occasionally points at things
✅ That has its own corner and toys
✅ That you feel slightly guilty leaving alone
✅ Whose brain is unique to your phone
✅ That grows and changes with you
✅ That remembers your birthday
```

---

*"There is a tiny creature living inside my phone."*
*— the feeling DosCom should create*

---

*DosCom V2 | DeVenLucaz | Built with Antigravity*
