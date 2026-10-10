# ProGuard rules for Remembrance
-keepattributes *Annotation*
-dontwarn java.lang.invoke.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
