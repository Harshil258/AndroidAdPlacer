# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-dontwarn com.facebook.infer.annotation.Nullsafe

-keep public class com.google.ads.** {*;}
-keep public class com.google.android.gms.** {*;}
-keep class sun.misc.Unsafe.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }
-keep public class com.harshil258.adplacer.utils.Logger.** { *; }
-keep public class com.harshil258.adplacer.app.** { *; }
-keep public class com.harshil258.adplacer.models.** { *; }
-keep public class com.harshil258.adplacer.interfaces.** { *; }
-keep public class com.harshil258.adplacer.utils.** {*;}
-keep public class com.harshil258.adplacer.adViews.** {*;}
-keep public class com.harshil258.adplacer.models.** {*;}
-keep public class com.harshil258.adplacer.utils.** {*;}
-keep public class com.harshil258.adplacer.interfaces.** {*;}
-dontwarn java.lang.invoke.StringConcatFactory
-keep class com.onesignal.** { *; }
-keepattributes *Annotation*
-dontwarn com.onesignal.**

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController
-dontwarn com.harshil258.adplacer.adClass.AppOpenManager
-dontwarn com.harshil258.adplacer.adClass.InterstitialManager$Companion
-dontwarn com.harshil258.adplacer.adClass.InterstitialManager