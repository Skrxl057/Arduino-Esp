# Proguard rules for EmbedHub
-keep class com.embedhub.app.core.model.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.eclipse.paho.client.mqttv3.**
-dontwarn com.hoho.android.usbserial.**
