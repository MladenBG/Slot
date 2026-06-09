# Keep JNI native methods
-keep class com.magics.slot.SlotNativeBridge { *; }
-keep class com.magics.slot.** { *; }
-keepclassmembers class * {
    native <methods>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class androidx.compose.** { *; }
