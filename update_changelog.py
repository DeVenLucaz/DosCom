import re

file_path = "/data/data/com.termux/files/home/DosCom/CHANGELOG.md"
with open(file_path, "r") as f:
    content = f.read()

new_log = """## [2.0.0-alpha.13] - 2026-06-08
### Added (V2 Phase 12 Complete — Awake Mode Gemini Voice)
- Created `ConversationHistory` to maintain a rolling window of the last 10 conversational exchanges for context.
- Updated `GeminiVisionClient` with a new `speak()` function to inject personality, user mood, app context, and chat history into the LLM system instructions.
- Patched `CompanionOverlayService` to intelligently intercept user queries during AWAKE mode, forwarding them to Gemini and rendering the returned conversational text inside the speech bubble.
- Rewarded the DosCombrain system positively each time the user interacts via chat in AWAKE mode.

"""

content = content.replace("# Changelog\n", "# Changelog\n\n" + new_log)

with open(file_path, "w") as f:
    f.write(content)
