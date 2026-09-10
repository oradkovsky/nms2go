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

# Optional classes referenced by POI/XMLBeans that are not available on Android.
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.javaparser.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.sun.org.apache.xml.internal.resolver.**
-dontwarn de.rototor.pdfbox.graphics2d.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn java.lang.invoke.**
-dontwarn javax.imageio.**
-dontwarn javax.security.**
-dontwarn javax.swing.**
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.stream.**
-dontwarn kxml2.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.harmony.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.pdfbox.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.xml.security.**
-dontwarn org.bouncycastle.**
-dontwarn org.brotli.**
-dontwarn org.checkerframework.**
-dontwarn org.joda.time.**
-dontwarn org.etsi.uri.**
-dontwarn org.ietf.jgss.**
-dontwarn org.osgi.annotation.**
-dontwarn org.osgi.framework.**
-dontwarn org.slf4j.**
-dontwarn org.tukaani.xz.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn org.openxmlformats.schemas.**

# POI and its dependencies use reflection and service loaders at runtime,
# so keep them intact while the app's own code stays minified.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.commons.** { *; }
-keep class org.apache.logging.log4j.** { *; }
-keep class org.etsi.uri.** { *; }
-keep class org.w3.x2000.x09.xmldsig.** { *; }
-keep class schemasMicrosoftComVml.** { *; }
-keep class net.sourceforge.jexcelapi.** { *; }