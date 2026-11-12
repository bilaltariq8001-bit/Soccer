# kotlinx.serialization: keep generated serializers for the DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.innovativeinside.pitchside.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.innovativeinside.pitchside.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
