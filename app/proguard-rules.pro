-keep class com.devenlucaz.doscom.brain.** { *; }
-keep class com.devenlucaz.doscom.animation.** { *; }
-keep class com.devenlucaz.doscom.systems.** { *; }
-keep class com.devenlucaz.doscom.personality.** { *; }
-keep class com.devenlucaz.doscom.events.** { *; }
-keep class com.devenlucaz.doscom.mode.** { *; }

-keepclassmembers class * {
    ** copy(...);
}

-keepclassmembers class * {
    <fields>;
    <init>(...);
    <methods>;
}
