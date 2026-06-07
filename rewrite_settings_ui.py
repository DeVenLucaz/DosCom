import os

xml_file = "app/src/main/res/layout/activity_settings.xml"
with open(xml_file, "r") as f:
    content = f.read()

# Replace CardView with LinearLayout + glass background
content = content.replace("<androidx.cardview.widget.CardView", "<LinearLayout")
content = content.replace("</androidx.cardview.widget.CardView>", "</LinearLayout>")
content = content.replace("app:cardBackgroundColor=\"#1A1A2E\"", "android:background=\"@drawable/glass_card\"")
content = content.replace("app:cardCornerRadius=\"16dp\"", "")

# Add CompanionRenderer to mode cards
def add_renderer(card_id):
    global content
    target = f'<LinearLayout\n                        android:id="@+id/{card_id}"'
    replacement = f'<LinearLayout\n                        android:id="@+id/{card_id}"'
    
    # find where to insert
    import re
    # We want to replace the emoji TextView with CompanionRenderer
    pattern = rf'(<LinearLayout[^>]*?android:id="@+id/{card_id}"[^>]*?>.*?)\s*<TextView[^>]*?android:text="[🐾💬🧠]"[^>]*?/>'
    
    match = re.search(pattern, content, re.DOTALL)
    if match:
        content = content[:match.start()] + match.group(1) + f'\n                        <com.devenlucaz.doscom.character.CompanionRenderer android:id="@+id/render_{card_id}" android:layout_width="60dp" android:layout_height="60dp"/>' + content[match.end():]

add_renderer("cardAlive")
add_renderer("cardAwake")
add_renderer("cardAware")

# Update SeekBars with custom progress drawable
content = content.replace("<SeekBar\n                    android:id=\"@+id/seekMascotSize\"", "<SeekBar\n                    android:id=\"@+id/seekMascotSize\"\n                    android:progressDrawable=\"@drawable/slider_track\"")
content = content.replace("<SeekBar\n                    android:id=\"@+id/seekAnimSpeed\"", "<SeekBar\n                    android:id=\"@+id/seekAnimSpeed\"\n                    android:progressDrawable=\"@drawable/slider_track\"")

with open(xml_file, "w") as f:
    f.write(content)

os.makedirs("app/src/main/res/drawable", exist_ok=True)

with open("app/src/main/res/drawable/glass_card.xml", "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#991A1A2E" />
    <corners android:radius="16dp" />
    <stroke android:width="1dp" android:color="#40FFFFFF" />
</shape>""")

with open("app/src/main/res/drawable/slider_track.xml", "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background">
        <shape>
            <corners android:radius="4dp"/>
            <solid android:color="#33FFFFFF"/>
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <clip>
            <shape>
                <corners android:radius="4dp"/>
                <gradient
                    android:startColor="#7B2FBE"
                    android:endColor="#00B4FF"
                    android:angle="0" />
            </shape>
        </clip>
    </item>
</layer-list>""")
