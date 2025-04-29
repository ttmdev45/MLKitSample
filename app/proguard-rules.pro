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

# Preserve ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }

# Preserve Firebase ML Kit classes if you're using Firebase versions
-keep class com.google.firebase.ml.** { *; }
-keep interface com.google.firebase.ml.** { *; }

# Keep annotations that ML Kit uses
-keepattributes *Annotation*

# Needed for internal Firebase dependencies if used
-keep class com.google.protobuf.** { *; }
-keep interface com.google.protobuf.** { *; }

# Optional: Keep the metadata classes (important for custom models)
-keepclassmembers class ** {
    @com.google.firebase.encoders.annotations.Encodable <fields>;
}

# If you use ML Kit's custom models (local or hosted)
-keep class com.google.mlkit.common.model.DownloadConditions { *; }
-keep class com.google.mlkit.common.model.RemoteModel { *; }
-keep class com.google.mlkit.common.model.CustomRemoteModel { *; }
#-keep class com.google.mlkit.common.model.CustomLocalModel { *; }

# If you use Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }

# If you use Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }

# If you use Face Detection
-keep class com.google.mlkit.vision.face.** { *; }

# If you use Image Labeling
-keep class com.google.mlkit.vision.label.** { *; }

# If you use Object Detection and Tracking
-keep class com.google.mlkit.vision.objects.** { *; }

# For Image Processing
-keep class com.google.mlkit.vision.common.** { *; }

# General ML Kit vision rules
-keep class com.google.mlkit.vision.** { *; }
