# Proguard rules for Friendai / Mom's AI

# Keep the app's own classes (all Kotlin data classes, services, activities, etc.)
-keep class com.friendai.** { *; }

# Keep the CaregiverSettings data class fields so SharedPreferences key constants survive
-keepclassmembers class com.friendai.CaregiverSettings {
    *;
}

# Keep CompanionReply and EscalationLevel (used as callback return values)
-keep class com.friendai.CompanionReply { *; }
-keep enum  com.friendai.EscalationLevel { *; }

# Keep RecognitionListener impl in TimedListeningService (called via Android SpeechRecognizer)
-keep class com.friendai.TimedListeningService { *; }
-keepclassmembers class com.friendai.TimedListeningService {
    public *;
}

# Keep TextToSpeech.OnInitListener impl
-keepclassmembers class * implements android.speech.tts.TextToSpeech$OnInitListener {
    public void onInit(int);
}

# Keep UtteranceProgressListener subclasses (anonymous inner classes in service and activity)
-keep class * extends android.speech.tts.UtteranceProgressListener { *; }

# Keep Android Activity/Service/BroadcastReceiver entry points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Play Billing — keep all public API classes
-keep class com.android.billingclient.** { *; }

# JSON — keep fields for org.json (used in ProxyAiClient)
-keep class org.json.** { *; }

# OkHttp / HttpURLConnection (used in ProxyAiClient network calls)
-dontwarn okhttp3.**
-dontwarn okio.**

# General Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
