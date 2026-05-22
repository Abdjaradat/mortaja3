-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.raed.app.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
