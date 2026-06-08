import re
file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace from 'idleEngine.targetState.bodyOffsetY = 0f\n            },'
# up to the end of the ChatInputOverlay constructor
pattern = re.compile(r'idleEngine\.targetState\.bodyOffsetY = 0f\n\s*},\n\s*onReactedPositive = \{.*?\}\n\s*\)', re.DOTALL)

replacement = 'idleEngine.targetState.bodyOffsetY = 0f\n            }\n        )'

content = pattern.sub(replacement, content)

with open(file_path, "w") as f:
    f.write(content)
print("Done")

