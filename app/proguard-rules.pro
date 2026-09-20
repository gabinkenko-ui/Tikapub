# Retrofit / Moshi / OkHttp keep rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.gabinkenko.tikapub.tiktok.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**
