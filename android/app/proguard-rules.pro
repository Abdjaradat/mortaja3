# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlinx Serialization
-keepattributes InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.raed.app.**$$serializer { *; }
-keepclassmembers class com.raed.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.raed.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gson/Models (kept for any transitive deps)
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.android.* <methods>;
}

# Firebase
-keep class com.google.firebase.** { *; }

# App model classes (navigation args, API models)
-keep class com.raed.app.** { *; }
