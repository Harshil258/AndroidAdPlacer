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




-keep public class com.google.ads.** {*;}
-keep public class com.google.android.gms.** {*;}
-keep class sun.misc.Unsafe.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }
-keep public class com.harshil258.adplacer.Ads.utils.Logger.** { *; }
-keep public class com.harshil258.adplacer.app.** { *; }
-keep public class com.harshil258.adplacer.models.** { *; }
-keep public class com.harshil258.adplacer.interfaces.** { *; }
-keep public class com.harshil258.adplacer.Ads.utils.** {*;}
-keep public class com.harshil258.adplacer.adViews.** {*;}
-keep public class com.harshil258.adplacer.Ads.models.** {*;}
-keep public class com.harshil258.adplacer.utils.** {*;}
-keep public class com.harshil258.adplacer.Ads.interfaces.** {*;}
-dontwarn java.lang.invoke.StringConcatFactory
-keep class com.onesignal.** { *; }
-keepattributes *Annotation*
-dontwarn com.onesignal.**