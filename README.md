# DosCom

> *"DosCom is not an assistant pretending to be alive.*
> *It is a tiny living creature that occasionally helps."*

DosCom is a free Android companion that lives on your screen as a floating overlay. It is not a chatbot with a face. It is a creature first — curious, playful, emotionally present — that happens to occasionally be useful.

It reacts to your phone, notices what you're doing, entertains itself when you're busy, and remembers how interactions make it feel. The AI is invisible infrastructure. You should never think "the AI detected something." You should think "it noticed."

---

## Install

> **You must install DosCom through [Obtainium](https://github.com/ImranR98/Obtainium) — not by downloading the APK directly.**
>
> Android blocks Accessibility Service for APKs sideloaded directly from a browser or file manager. Obtainium installs apps the same way F-Droid does — Android treats it as a proper app source and the restriction is lifted. AWARE mode will not work without this.

**Step 1 — Install Obtainium**
Download and install Obtainium from [obtainium.imranr.dev](https://obtainium.imranr.dev) or its [GitHub Releases](https://github.com/ImranR98/Obtainium/releases).

**Step 2 — Add DosCom**
Open Obtainium → Add App → paste this URL:
```
https://github.com/DeVenLucaz/DosCom
```

**Step 3 — Install and run**
Install the latest release through Obtainium, open DosCom, and complete onboarding.

> **Min Android:** 10 (API 29) · **Target:** Android 13+

---

## The Three Modes

Switch modes by **double tapping** DosCom. Triple tap opens Settings.

### ALIVE 🐾
Overlay permission only. No API. No accessibility. No mic.

The creature exists. It animates, reacts to phone events, entertains itself, and notices the world around it. Pure companion. Zero utility pressure.

Antenna glows **white**.

### AWAKE 💬
Everything in ALIVE, plus a Gemini API key.

The creature can speak. Rarely. In toddler language. Not an assistant — just a creature that sometimes has something to say. Gemini drives the voice. Invisibly.

Antenna glows **blue**.

### AWARE 🧠
Everything in AWAKE, plus Accessibility permission.

The creature has an extra sense. It notices what app is open, reacts to what's on screen, and occasionally points at things like a toddler going "look! look!" Accessibility is a sense, not a feature.

Antenna glows **green**.

---

## What DosCom Does

### It moves like a creature
DosCom travels across your screen using a mime movement system — it selects a style based on distance, direction, mood, and time of day. Walk, slide, staircase, skateboard, tightrope, balloon, rocket, moonwalk. 1% random moonwalk regardless of all other conditions.

It climbs edges. It slips sometimes (15% chance per 5 cycles). It occasionally stops mid-climb to rest and decides whether to continue or slide back down.

### It has a life when nothing is happening
A full idle animation catalog runs continuously when DosCom isn't busy: subtle body bob, blinking, antenna glow pulse, and random sub-animations — stretches, sneezes, hiccups, yawns, coin flips, phone checks. Sleep kicks in after your configured timer. Zzz particles float upward. It wakes instantly on any touch.

### It reacts to your phone
- **Charging:** sprints to the bottom, sits down, mimes plugging in, shows a battery bar on its chest
- **Low battery:** collapses, recovers, then runs to charge
- **Headphones:** pulls out a boombox, dances while connected
- **Screenshot:** photobombs
- **Keyboard open:** peeks over the top
- **Silent mode:** moves in exaggerated tiptoe
- **Airplane mode:** puts on a pilot hat
- **Shake:** grabs the nearest edge and glares
- **Walking detected:** bounces in rhythm with your steps

### It has toys
When bored, DosCom pulls out toys from its toy box — fishing rod, magnifying glass, treasure map, sword, binoculars, book. Each toy has its own full animation sequence. The book can put it to sleep. The map sends it on a systematic screen exploration. The magnifying glass triggers a discovery sequence where it finds imaginary artifacts and holds them up for you to react to.

### It has a brain
Every install of DosCom has a unique Spiking Neural Network (Leaky Integrate-and-Fire) that makes behavioral decisions — which toy to pick, which mime to use, how often to trigger discoveries. The brain learns from your reactions and saves its weights. No two DosComs behave identically over time.

### It remembers how it feels
DosCom tracks a persistent emotional sentiment score. Positive reactions make it more confident and energetic. Negative reactions make it quieter and more withdrawn. It recovers — toddlers forgive fast. Long press DosCom to open the reaction box.

### It knows your birthday
Set your birthday in Settings and DosCom runs a full day persona: midnight confetti, morning gift box, afternoon tiny cake, evening party hat at a slight tilt. DosCom also has its own birthday (install date). It eats the whole cake itself and gets embarrassed if you tap it that day.

---

## The Robot

DosCom is drawn entirely in code using Android Canvas. No image assets. No spritesheets. Chibi proportions, silver color scheme, expressive face system — open/closed/wide eyes, pupil direction, happy/worried/neutral mouth, blush, tongue, and props all rendered live every frame.

Pinch to resize. The scale preference saves between sessions.

---

## Tech Stack

Kotlin · Canvas · WindowManager · AccessibilityService · Gemini API · Spiking Neural Network (pure Kotlin LIF) · SharedPreferences · Choreographer · GitHub Actions CI/CD

---

## Build from Source

```bash
git clone https://github.com/DeVenLucaz/DosCom
cd DosCom
./gradlew assembleRelease
```

Requires: Android Studio, JDK 17, Android SDK 34+. CI builds on every push to `main`. Signed release builds on version tags.

---

## Status

| Phase | Description | Status |
|---|---|---|
| 1 | Mode system + Settings | ✅ Complete |
| 2a | Static robot drawing | ✅ Complete |
| 2b | Animation state + limbs | ✅ Complete |
| 3 | Position-aware poses | ✅ Complete |
| 4 | Climb + mime movement | ✅ Complete |
| 5 | Idle animations + sleep | ✅ Complete |
| 6 | Toy box + self entertainment | ✅ Complete |
| 7 | Phone event reactions | ✅ Complete |
| 8 | DosCom Brain (SNN) | ✅ Complete |
| 9 | Emotional memory + reactions | ✅ Complete |
| 10 | Birthday system | ✅ Complete |
| 11 | Glassmorphism UI | ✅ Complete |
| 12 | AWAKE mode (Gemini voice) | ✅ Complete |
| 13 | AWARE mode (screen sense) | ✅ Complete |
| 14 | Touch modes + pinch resize | ✅ Complete |
| 15 | Onboarding V2 | ✅ Complete |
| 16 | Final polish | ✅ Complete |
| — | **V2 Testing** | 🔄 In progress |

---

## Other Projects

| Project | What it does |
|---|---|
| 💣 **[MINEPATH](https://github.com/DeVenLucaz/minepath)** | Browser minesweeper survival game. One chicken. One minefield. No second chances. |
| 🦙 **[llamdrop](https://github.com/DeVenLucaz/llamdrop)** | Run local AI on any device — Android, Linux, Raspberry Pi. Reads your hardware, picks the right model, runs it. |
| 🟢 **[Vernux](https://github.com/DeVenLucaz/Vernux)** | Natural language interface for Termux. Type what you want in plain English. 700+ commands, offline AI fallback. |

---

## License

Source Available — All Rights Reserved.

---

## Built By

**DeVen — [DeVenLucaz](https://github.com/DeVenLucaz)**
Independent creator. Pune, India.
Building games, tools, music, and interactive fiction entirely on Android.

*There is a tiny creature living inside my phone.*
