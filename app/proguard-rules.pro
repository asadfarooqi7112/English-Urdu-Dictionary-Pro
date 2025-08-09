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

-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

# Gson rules to preserve generic type information and fields
-keepattributes Signature
-keepattributes *Annotation*

# Keep all classes that extend TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }

# Keep all fields and methods of your data model classes
# Replace `com.hfad.personaldictionary.models.*` with the actual package
# where your data model classes (like 'YourObject' from the previous example) are located.
-keep class com.hfad.personaldictionary.models.** { *; }
