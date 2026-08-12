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

# AdMob / Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.ads.**

# Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Retrofit: usa interfaces con generics y anotaciones via reflexion en tiempo de ejecucion.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface com.uaa.misgastosapp.network.GastosApiService { *; }
-dontwarn retrofit2.**

# Gson: deserializa los modelos de red/perfil via reflexion, necesita conservar sus campos.
-keepclassmembers class com.uaa.misgastosapp.model.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# SQLCipher: JNI nativo, evita que R8 renombre/elimine algo que rompa la carga de la libreria.
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**